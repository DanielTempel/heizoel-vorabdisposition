package heizoel.backend.adapter.out.web.dispo;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.NotificationDeliveryStatus;
import heizoel.backend.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@AutoConfigureMockMvc
class DispoControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @MockitoBean
    NotificationService notificationService;

    @BeforeEach
    void cleanDatabase() {
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();
        reset(notificationService);
    }

    @Test
    void emailRequestReturnsAcceptedAndPersistsPendingAggregate() throws Exception {
        performCreate(request("ORDER-EMAIL", CommunicationChannel.EMAIL))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId").value("ORDER-EMAIL"))
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        Order order = orderRepository.findAll().get(0);
        ConfirmationRequest request = confirmationRequestRepository.findAll().get(0);
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(order.getCustomerEmail()).isEqualTo("customer@example.com");
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(request.isActive()).isFalse();
        assertThat(request.getSentAt()).isNull();
        assertThat(request.getExpiresAt()).isNull();
        verifyNoInteractions(notificationService);
    }

    @Test
    void smsRequestReturnsAcceptedAndPersistsPhoneNumber() throws Exception {
        performCreate(request("ORDER-SMS", CommunicationChannel.SMS))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        Order order = orderRepository.findAll().get(0);
        ConfirmationRequest request = confirmationRequestRepository.findAll().get(0);
        assertThat(order.getCustomerEmail()).isNull();
        assertThat(order.getCustomerPhoneNumber()).isEqualTo("+491701234567");
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(request.isPending()).isTrue();
        verifyNoInteractions(notificationService);
    }

    @Test
    void identicalPendingRequestIsAcceptedWithoutCreatingDuplicate() throws Exception {
        TestDispoRequest request = request("ORDER-DUPLICATE", CommunicationChannel.EMAIL);

        performCreate(request).andExpect(status().isAccepted());
        performCreate(request)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(confirmationRequestRepository.count()).isEqualTo(1);
        verifyNoInteractions(notificationService);
    }

    @Test
    void invalidDeliveryIntervalReturnsValidationError() throws Exception {
        TestDispoRequest request = request("ORDER-INVALID-SLOT", CommunicationChannel.EMAIL)
                .withDelivery("2099-06-12", "14:00", "13:00");

        performCreate(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Delivery window start must be before delivery window end."));

        assertThat(orderRepository.count()).isZero();
        assertThat(confirmationRequestRepository.count()).isZero();
    }

    @Test
    void pastDeliveryWindowIsAcceptedForAsynchronousSendValidation() throws Exception {
        TestDispoRequest request = request("ORDER-PAST-SLOT", CommunicationChannel.EMAIL)
                .withDelivery(LocalDate.now().minusDays(1).toString(), "10:00", "11:00");

        performCreate(request)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        assertThat(confirmationRequestRepository.findAll().get(0).isPending()).isTrue();
        verifyNoInteractions(notificationService);
    }

    @Test
    void emailWithoutAddressReturnsMissingDigitalContact() throws Exception {
        TestDispoRequest request = request("ORDER-NO-EMAIL", CommunicationChannel.EMAIL)
                .withContacts("", null);

        performCreate(request)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MISSING_DIGITAL_CONTACT"));
    }

    @Test
    void smsWithoutPhoneReturnsMissingDigitalContact() throws Exception {
        TestDispoRequest request = request("ORDER-NO-PHONE", CommunicationChannel.SMS)
                .withContacts(null, "");

        performCreate(request)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MISSING_DIGITAL_CONTACT"));
    }

    @Test
    void missingCommunicationChannelReturnsValidationError() throws Exception {
        performCreate(request("ORDER-NO-CHANNEL", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void responseDeadlineOutsideAllowedRangeIsRejected() throws Exception {
        performCreate(request("ORDER-ZERO-DEADLINE", CommunicationChannel.EMAIL).withDeadline(0))
                .andExpect(status().isBadRequest());
        performCreate(request("ORDER-LARGE-DEADLINE", CommunicationChannel.EMAIL).withDeadline(169))
                .andExpect(status().isBadRequest());

        assertThat(confirmationRequestRepository.count()).isZero();
    }

    @Test
    void maximumResponseDeadlineIsAcceptedAndStoredPending() throws Exception {
        performCreate(request("ORDER-MAX-DEADLINE", CommunicationChannel.EMAIL).withDeadline(168))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        List<ConfirmationRequest> requests = confirmationRequestRepository.findAll();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getResponseDeadlineHours()).isEqualTo(168);
        assertThat(requests.get(0).getExpiresAt()).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            TestDispoRequest request
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }



    private TestDispoRequest request(String externalOrderId, CommunicationChannel channel) {
        return new TestDispoRequest(
                externalOrderId,
                "17",
                "WUE-AB 123",
                "Max Muller",
                channel == CommunicationChannel.SMS ? null : "customer@example.com",
                channel == CommunicationChannel.SMS ? "+491701234567" : null,
                channel,
                "Beispielstrasse 12, 97070 Wuerzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heating oil",
                3_000,
                "2099-06-12",
                "10:00",
                "11:00",
                24,
                "100 EUR"
        );
    }

    private record TestDispoRequest(
            String externalOrderId,
            String tourNumber,
            String vehicleLicensePlate,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            CommunicationChannel communicationChannel,
            String deliveryAddress,
            Double locationX,
            Double locationY,
            Double targetLocationX,
            Double targetLocationY,
            String product,
            Integer quantityLiters,
            String deliveryDate,
            String deliveryWindowStart,
            String deliveryWindowEnd,
            Integer responseDeadlineHours,
            String priceDisplayText
    ) {
        private TestDispoRequest withDelivery(String date, String start, String end) {
            return new TestDispoRequest(
                    externalOrderId, tourNumber, vehicleLicensePlate, customerName,
                    customerEmail, customerPhoneNumber, communicationChannel,
                    deliveryAddress, locationX, locationY, targetLocationX, targetLocationY,
                    product, quantityLiters, date, start, end, responseDeadlineHours,
                    priceDisplayText
            );
        }

        private TestDispoRequest withContacts(String email, String phone) {
            return new TestDispoRequest(
                    externalOrderId, tourNumber, vehicleLicensePlate, customerName,
                    email, phone, communicationChannel, deliveryAddress,
                    locationX, locationY, targetLocationX, targetLocationY,
                    product, quantityLiters, deliveryDate, deliveryWindowStart,
                    deliveryWindowEnd, responseDeadlineHours, priceDisplayText
            );
        }

        private TestDispoRequest withDeadline(int hours) {
            return new TestDispoRequest(
                    externalOrderId, tourNumber, vehicleLicensePlate, customerName,
                    customerEmail, customerPhoneNumber, communicationChannel,
                    deliveryAddress, locationX, locationY, targetLocationX, targetLocationY,
                    product, quantityLiters, deliveryDate, deliveryWindowStart,
                    deliveryWindowEnd, hours, priceDisplayText
            );
        }
    }
}
