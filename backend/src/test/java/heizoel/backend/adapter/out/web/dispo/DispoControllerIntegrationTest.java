package heizoel.backend.adapter.out.web.dispo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import heizoel.backend.adapter.in.web.overview.dto.ResendConfirmationRequestRequestDto;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.NotificationDeliveryStatus;
import heizoel.backend.domain.Order;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/test/configure-test-company.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class DispoControllerIntegrationTest {

    private static final String CONFIRMATION_PROCESS_KEY = "confirmation-request-process";
    private static final String SEND_ACTIVITY = "ServiceTask_SendConfirmationRequest";
    private static final String MARK_FAILED_ACTIVITY = "ServiceTask_MarkDeliveryFailed";
    private static final String TEST_API_KEY = "test-minova-api-key";

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

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    ManagementService managementService;

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
    void pastDeliveryWindowIsRejectedBeforePersistence() throws Exception {
        TestDispoRequest request = request("ORDER-PAST-SLOT", CommunicationChannel.EMAIL)
                .withDelivery("2000-01-01", "10:00", "11:00");

        performCreate(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Delivery window must start in the future."));

        assertThat(orderRepository.count()).isZero();
        assertThat(confirmationRequestRepository.count()).isZero();
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
    void nonPositiveResponseDeadlineIsRejected() throws Exception {
        performCreate(request("ORDER-ZERO-DEADLINE", CommunicationChannel.EMAIL).withDeadline(0))
                .andExpect(status().isBadRequest());

        assertThat(confirmationRequestRepository.count()).isZero();
    }

    @Test
    void responseDeadlineAboveFormerMaximumIsAcceptedAndStoredPending()
            throws Exception {
        performCreate(request("ORDER-LARGE-DEADLINE", CommunicationChannel.EMAIL).withDeadline(169))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        List<ConfirmationRequest> requests = confirmationRequestRepository.findAll();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getResponseDeadlineHours()).isEqualTo(169);
        assertThat(requests.get(0).getExpiresAt()).isNull();
    }

    @Test
    void resendEndpointAfterDeliveryFailureReturnsAcceptedAndStartsPendingWorkflow()
            throws Exception {
        performCreate(
                request("ORDER-RESEND", CommunicationChannel.EMAIL)
                        .withContacts(
                                "customer@example.com",
                                "+491701234567"
                        )
        ).andExpect(status().isAccepted());

        Order order = orderRepository.findAll().get(0);
        ConfirmationRequest oldRequest =
                confirmationRequestRepository.findTopByOrderOrderByIdDesc(order)
                        .orElseThrow();
        Long oldRequestId = oldRequest.getId();
        DeliverySlot oldDeliverySlot = oldRequest.getDeliverySlot();

        ProcessInstance oldProcess = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(CONFIRMATION_PROCESS_KEY)
                .processInstanceBusinessKey(oldRequestId.toString())
                .singleResult();
        assertThat(oldProcess).isNotNull();

        Job oldSendJob = managementService
                .createJobQuery()
                .processInstanceId(oldProcess.getId())
                .activityId(SEND_ACTIVITY)
                .singleResult();
        assertThat(oldSendJob).isNotNull();

        doThrow(new EmailSettingsNotConfiguredException(
                "Mail sender is not configured"
        )).when(notificationService).sendConfirmationRequest(
                any(Order.class),
                any(ConfirmationRequest.class)
        );

        managementService.executeJob(oldSendJob.getId());

        Job markFailedJob = managementService
                .createJobQuery()
                .processInstanceId(oldProcess.getId())
                .activityId(MARK_FAILED_ACTIVITY)
                .singleResult();
        assertThat(markFailedJob).isNotNull();
        managementService.executeJob(markFailedJob.getId());

        ConfirmationRequest failedRequest = confirmationRequestRepository
                .findById(oldRequestId)
                .orElseThrow();
        assertThat(failedRequest.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(failedRequest.isActive()).isFalse();
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(oldProcess.getId())
                .count()).isZero();

        MockHttpSession session = authenticatedDashboardSession();
        CsrfData csrf = fetchCsrfToken(session);

        mockMvc.perform(post(
                        "/api/dashboard/orders/{externalOrderId}/resend",
                        "ORDER-RESEND"
                )
                        .session(session)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendConfirmationRequestRequestDto(
                                        CommunicationChannel.SMS,
                                        24
                                )
                        )))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId").value("ORDER-RESEND"))
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        ConfirmationRequest updatedOldRequest =
                confirmationRequestRepository.findById(oldRequestId).orElseThrow();
        ConfirmationRequest newRequest =
                confirmationRequestRepository
                        .findTopByOrderOrderByIdDesc(updatedOrder)
                        .orElseThrow();

        assertThat(updatedOldRequest.isActive()).isFalse();
        assertThat(newRequest.getId()).isNotEqualTo(oldRequestId);
        assertThat(newRequest.getCommunicationChannel())
                .isEqualTo(CommunicationChannel.SMS);
        assertThat(newRequest.getResponseDeadlineHours()).isEqualTo(24);
        assertThat(newRequest.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(newRequest.getDeliverySlot()).isEqualTo(oldDeliverySlot);
        assertThat(updatedOrder.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.OPEN);

        ProcessInstance newProcess = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(CONFIRMATION_PROCESS_KEY)
                .processInstanceBusinessKey(newRequest.getId().toString())
                .singleResult();
        assertThat(newProcess).isNotNull();
    }

    private MockHttpSession authenticatedDashboardSession() throws Exception {
        MvcResult accessResult = mockMvc.perform(
                        post("/api/dispo/dashboard-access")
                                .header("X-API-Key", TEST_API_KEY)
                )
                .andExpect(status().isOk())
                .andReturn();

        String code = UriComponentsBuilder
                .fromUriString(accessResult.getResponse().getContentAsString())
                .build()
                .getQueryParams()
                .getFirst("code");
        assertThat(code).isNotBlank();

        MvcResult exchangeResult = mockMvc.perform(
                        post("/api/dashboard/auth/exchange")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(code)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(exchangeResult.getRequest().getSession(false))
                .isInstanceOf(MockHttpSession.class);
        return (MockHttpSession) exchangeResult.getRequest().getSession(false);
    }

    private CsrfData fetchCsrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/dashboard/csrf").session(session)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return new CsrfData(
                response.path("headerName").asText(),
                response.path("token").asText()
        );
    }

    private record CsrfData(String headerName, String token) {
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            TestDispoRequest request
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .header("X-API-Key", TEST_API_KEY)
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
