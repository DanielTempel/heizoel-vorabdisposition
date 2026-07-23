package heizoel.backend.adapter.web.dispo;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.domain.*;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DispoControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @MockitoBean
    JavaMailSender javaMailSender;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OrderSnapshotRepository orderSnapshotRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    CustomerResponseRepository customerResponseRepository;

    @MockitoBean
    NotificationService notificationService;

    @BeforeEach
    void cleanDatabase() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderSnapshotRepository.deleteAll();

        Mockito.reset(notificationService);
    }

    @Test
    void createConfirmationRequest_emailChannel_createsOrderSnapshotAndConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<Order> orders = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orders).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);

        Order order = orders.get(0);
        ConfirmationRequest confirmationRequest = confirmationRequests.get(0);

        assertThat(order.getExternalOrderId()).isEqualTo("A-1024");
        assertThat(order.getCustomerName()).isEqualTo("Max Muller");
        assertThat(order.getCustomerEmail()).isEqualTo("daniel@example.com");
        assertThat(order.getCustomerPhoneNumber()).isNull();
        assertThat(order.getDeliveryAddress()).isEqualTo("Beispielstrase 12, 97070 Wurzburg");
        assertThat(order.getProduct()).isEqualTo("Heizol");
        assertThat(order.getQuantityLiters()).isEqualTo(3000);
        assertThat(order.getPriceDisplayText()).isEqualTo("100 EUR");
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        assertThat(confirmationRequest.getOrder().getId()).isEqualTo(order.getId());
        assertThat(confirmationRequest.getToken()).isNotBlank();
        assertThat(confirmationRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(confirmationRequest.getDeliveryDate()).hasToString("2099-06-12");
        assertThat(confirmationRequest.getDeliveryWindowStart()).hasToString("10:00");
        assertThat(confirmationRequest.getDeliveryWindowEnd()).hasToString("11:00");
        assertThat(confirmationRequest.getResponseDeadlineHours()).isEqualTo(24);
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(confirmationRequest.getSentAt()).isNotNull();
        assertThat(confirmationRequest.getExpiresAt()).isAfter(confirmationRequest.getSentAt());

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_smsChannel_createsOrderSnapshotAndConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(smsRequest("A-SMS-1024")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-SMS-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<Order> orders = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orders).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);

        Order order = orders.get(0);
        ConfirmationRequest confirmationRequest = confirmationRequests.get(0);

        assertThat(order.getExternalOrderId()).isEqualTo("A-SMS-1024");
        assertThat(order.getCustomerName()).isEqualTo("Max Muller");
        assertThat(order.getCustomerEmail()).isNull();
        assertThat(order.getCustomerPhoneNumber()).isEqualTo("+491701234567");
        assertThat(order.getPriceDisplayText()).isEqualTo("100 EUR");
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        assertThat(confirmationRequest.getOrder().getId()).isEqualTo(order.getId());
        assertThat(confirmationRequest.getToken()).isNotBlank();
        assertThat(confirmationRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(confirmationRequest.getResponseDeadlineHours()).isEqualTo(24);
        assertThat(confirmationRequest.isActive()).isTrue();

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_duplicateUnchangedEmailRequest_returnsOkAndDoesNotCreateSecondConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<Order> orders = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orders).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);
        assertThat(confirmationRequests.get(0).isActive()).isTrue();

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_sameAsConfirmedRequest_returnsOkAndDoesNotCreateSecondConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        markLatestRequestInactiveWithStatus("A-1024", ConfirmationStatus.CONFIRMED);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("CONFIRMED"));

        assertThat(orderSnapshotRepository.findAll()).hasSize(1);
        assertThat(confirmationRequestRepository.findAll()).hasSize(1);

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_sameAsRejectedRequest_returnsOkAndDoesNotCreateSecondConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        markLatestRequestInactiveWithStatus("A-1024", ConfirmationStatus.REJECTED);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("REJECTED"));

        assertThat(orderSnapshotRepository.findAll()).hasSize(1);
        assertThat(confirmationRequestRepository.findAll()).hasSize(1);

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_sameAsNoResponseRequest_createsNewConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        markLatestRequestInactiveWithStatus("A-1024", ConfirmationStatus.NO_RESPONSE);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshotRepository.findAll()).hasSize(1);
        assertThat(confirmationRequests).hasSize(2);
        assertThat(confirmationRequests)
                .filteredOn(ConfirmationRequest::isActive)
                .hasSize(1);

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_changedDeliveryWindow_invalidatesOldRequestAndCreatesNewRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "13:00", "14:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<Order> orders = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orders).hasSize(1);
        assertThat(confirmationRequests).hasSize(2);

        long activeCount = confirmationRequests.stream()
                .filter(ConfirmationRequest::isActive)
                .count();

        long inactiveCount = confirmationRequests.stream()
                .filter(request -> !request.isActive())
                .count();

        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);

        ConfirmationRequest activeRequest = confirmationRequests.stream()
                .filter(ConfirmationRequest::isActive)
                .findFirst()
                .orElseThrow();

        assertThat(activeRequest.getDeliveryWindowStart()).hasToString("13:00");
        assertThat(activeRequest.getDeliveryWindowEnd()).hasToString("14:00");
        assertThat(activeRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_changedCommunicationChannel_invalidatesOldRequestAndCreatesNewRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "10:00", "11:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(smsRequest("A-1024")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<Order> orders = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orders).hasSize(1);
        assertThat(confirmationRequests).hasSize(2);

        long activeCount = confirmationRequests.stream()
                .filter(ConfirmationRequest::isActive)
                .count();

        long inactiveCount = confirmationRequests.stream()
                .filter(request -> !request.isActive())
                .count();

        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);

        Order order = orders.get(0);

        assertThat(order.getCustomerEmail()).isNull();
        assertThat(order.getCustomerPhoneNumber()).isEqualTo("+491701234567");

        ConfirmationRequest activeRequest = confirmationRequests.stream()
                .filter(ConfirmationRequest::isActive)
                .findFirst()
                .orElseThrow();

        assertThat(activeRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_invalidDeliveryWindow_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequest("A-1024", "14:00", "13:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Delivery window start must be before delivery window end."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_deliveryWindowAlreadyStarted_returnsValidationError() throws Exception {
        String pastDeliveryDate = LocalDate.now(ZoneId.of("Europe/Berlin"))
                .minusDays(1)
                .toString();

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithDelivery(
                                "A-PAST-DELIVERY",
                                pastDeliveryDate,
                                "10:00",
                                "11:00",
                                24
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Delivery window must start in the future."))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_deadlineAfterDeliveryStart_capsExpiration() throws Exception {
        ZoneId deliveryZone = ZoneId.of("Europe/Berlin");
        LocalDate deliveryDate = LocalDate.now(deliveryZone).plusDays(1);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithDelivery(
                                "A-CAPPED-DEADLINE",
                                deliveryDate.toString(),
                                "10:00",
                                "11:00",
                                168
                        )))
                .andExpect(status().isCreated());

        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findAll()
                .get(0);

        assertThat(confirmationRequest.getResponseDeadlineHours()).isEqualTo(168);
        assertThat(confirmationRequest.getExpiresAt()).isEqualTo(
                deliveryDate.atTime(deliveryWindowStart).atZone(deliveryZone).toInstant()
        );
    }

    @Test
    void createConfirmationRequest_emailChannelWithoutCustomerEmail_returnsMissingDigitalContact() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithoutCustomerEmail()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MISSING_DIGITAL_CONTACT"))
                .andExpect(jsonPath("$.message").value("Customer e-mail is required when communication channel is EMAIL."))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_smsChannelWithoutCustomerPhoneNumber_returnsMissingDigitalContact() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(smsRequestWithoutCustomerPhoneNumber()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MISSING_DIGITAL_CONTACT"))
                .andExpect(jsonPath("$.message").value("Customer phone number is required when communication channel is SMS."))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_missingCommunicationChannel_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithoutCommunicationChannel()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_zeroResponseDeadlineHours_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithResponseDeadlineHours("A-DEADLINE-ZERO", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Response deadline in hours must be greater than 0."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"));

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_negativeResponseDeadlineHours_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithResponseDeadlineHours("A-DEADLINE-NEGATIVE", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Response deadline in hours must be greater than 0."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"));

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_tooLargeResponseDeadlineHours_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithResponseDeadlineHours("A-DEADLINE-TOO-LARGE", 169)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Response deadline must not exceed 168 hours."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"));

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void createConfirmationRequest_maxResponseDeadlineHours_createsRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailRequestWithResponseDeadlineHours("A-DEADLINE-MAX", 168)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-DEADLINE-MAX"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(confirmationRequests).hasSize(1);
        assertThat(confirmationRequests.get(0).getResponseDeadlineHours()).isEqualTo(168);
        assertThat(confirmationRequests.get(0).getExpiresAt())
                .isEqualTo(confirmationRequests.get(0).getSentAt().plusSeconds(168L * 60 * 60));

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
    }

    private String emailRequest(
            String externalOrderId,
            String deliveryWindowStart,
            String deliveryWindowEnd
    ) throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                externalOrderId,
                "Max Muller",
                "daniel@example.com",
                null,
                CommunicationChannel.EMAIL,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                deliveryWindowStart,
                deliveryWindowEnd,
                24,
                "100 EUR"
        ));
    }

    private String emailRequestWithDelivery(
            String externalOrderId,
            String deliveryDate,
            String deliveryWindowStart,
            String deliveryWindowEnd,
            Integer responseDeadlineHours
    ) throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                externalOrderId,
                "Max Muller",
                "daniel@example.com",
                null,
                CommunicationChannel.EMAIL,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                deliveryDate,
                deliveryWindowStart,
                deliveryWindowEnd,
                responseDeadlineHours,
                "100 EUR"
        ));
    }

    private String emailRequestWithResponseDeadlineHours(
            String externalOrderId,
            Integer responseDeadlineHours
    ) throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                externalOrderId,
                "Max Muller",
                "daniel@example.com",
                null,
                CommunicationChannel.EMAIL,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                "10:00",
                "11:00",
                responseDeadlineHours,
                "100 EUR"
        ));
    }

    private String smsRequest(
            String externalOrderId
    ) throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                externalOrderId,
                "Max Muller",
                null,
                "+491701234567",
                CommunicationChannel.SMS,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                "10:00",
                "11:00",
                24,
                "100 EUR"
        ));
    }

    private String emailRequestWithoutCustomerEmail() throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                "A-1024",
                "Max Muller",
                "",
                null,
                CommunicationChannel.EMAIL,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                "10:00",
                "11:00",
                24,
                "100 EUR"
        ));
    }

    private String smsRequestWithoutCustomerPhoneNumber() throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                "A-SMS-1024",
                "Max Muller",
                null,
                "",
                CommunicationChannel.SMS,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                "10:00",
                "11:00",
                24,
                "100 EUR"
        ));
    }

    private String requestWithoutCommunicationChannel() throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                "A-1024",
                "Max Muller",
                "daniel@example.com",
                null,
                null,
                "Beispielstrase 12, 97070 Wurzburg",
                9.8820,
                49.8166,
                9.9372,
                49.7935,
                "Heizol",
                3000,
                "2099-06-12",
                "10:00",
                "11:00",
                24,
                "100 EUR"
        ));
    }

    private void markLatestRequestInactiveWithStatus(
            String externalOrderId,
            ConfirmationStatus status
    ) {
        Order order = orderSnapshotRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findTopByOrderSnapshotOrderByIdDesc(order)
                .orElseThrow();

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);

        switch (status) {
            case SENT -> order.markSent();
            case CONFIRMED -> order.markConfirmed();
            case REJECTED -> order.markRejected();
            case NO_RESPONSE -> order.markNoResponse();
        }
        orderSnapshotRepository.save(order);
    }

    private record TestDispoRequest(
            String externalOrderId,
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
    }
}

