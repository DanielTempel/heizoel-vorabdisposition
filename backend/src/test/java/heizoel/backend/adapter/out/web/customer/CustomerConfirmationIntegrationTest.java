package heizoel.backend.adapter.out.web.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.in.web.customer.dto.CustomerResponseRequestDto;
import heizoel.backend.domain.*;
import heizoel.backend.application.port.out.location.GeocodingClient;
import heizoel.backend.application.port.out.location.LocationTrackingService;
import heizoel.backend.application.model.GeoCoordinate;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/test/configure-test-company.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
@Transactional
class CustomerConfirmationIntegrationTest {

    private static final String TEST_API_KEY = "test-minova-api-key";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Clock clock;

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

        registry.add("camunda.bpm.auto-deployment-enabled", () -> "true");
        registry.add("camunda.bpm.deployment-resource-pattern[0]", () -> "classpath*:processes/*.bpmn");
        registry.add("camunda.bpm.job-execution.enabled", () -> "false");

        registry.add("heizoel.confirmation.frontend-url", () -> "http://localhost:3000");
        registry.add("heizoel.confirmation.dispo-url", () -> "http://localhost:8090/api/dispo/confirmation-status-updates");
    }

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    ConfirmationWorkflowService confirmationWorkflowService;

    @MockitoBean
    LocationTrackingService locationTrackingService;

    @MockitoBean
    GeocodingClient geocodingClient;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    CustomerResponseRepository customerResponseRepository;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();

        Mockito.reset(
                notificationService,
                confirmationWorkflowService,
                locationTrackingService,
                geocodingClient
        );
        when(locationTrackingService.getDriverLocation(any()))
                .thenAnswer(invocation -> {
                    return java.util.Optional.of(new GeoCoordinate(9.8820D, 49.8166D));
                });
        when(geocodingClient.geocode(any()))
                .thenReturn(java.util.Optional.of(new GeoCoordinate(9.9372D, 49.7935D)));
    }

    @Test
    void getConfirmationPreview_returnsConfirmationDataByToken() throws Exception {
        String externalOrderId = "A-3001";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(get("/api/customer/confirmations/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId))
                .andExpect(jsonPath("$.customerName").value("Max Muller"))
                .andExpect(jsonPath("$.deliveryAddress").value("Beispielstrase 12, 97070 Wurzburg"))
                .andExpect(jsonPath("$.product").value("Heizol"))
                .andExpect(jsonPath("$.quantityLiters").value(3000))
                .andExpect(jsonPath("$.deliveryDate").value("2099-06-12"))
                .andExpect(jsonPath("$.deliveryWindowStart").value("10:00:00"))
                .andExpect(jsonPath("$.deliveryWindowEnd").value("11:00:00"))
                .andExpect(jsonPath("$.priceDisplayText").value("100 EUR"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));
    }

    @Test
    void getConfirmationPreview_rejectsOlderTokenAfterNewerRequestExists() throws Exception {
        String externalOrderId = "A-3001-LATEST-TOKEN";
        createDispoConfirmationRequest(externalOrderId);
        String olderToken = findActiveTokenByExternalOrderId(externalOrderId);
        String latestToken = createLaterRequest(externalOrderId);

        mockMvc.perform(get("/api/customer/confirmations/{token}", olderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/customer/confirmations/{token}", latestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId));
    }

    @Test
    void getTrackingInfo_returnsTrackingDataForDeliveryDate() throws Exception {
        String externalOrderId = "A-3001-TRACKING";

        createDispoConfirmationRequest(externalOrderId);
        setLatestDeliveryDate(externalOrderId, LocalDate.now(ZoneId.of("Europe/Berlin")));

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(get("/api/customer/confirmations/{token}/tracking-info", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingAvailable").value(true))
                .andExpect(jsonPath("$.targetLocationX").value(9.9372))
                .andExpect(jsonPath("$.targetLocationY").value(49.7935));
    }

    @Test
    void getDriverLocation_returnsDriverLocationForDeliveryDate() throws Exception {
        String externalOrderId = "A-3001-DRIVER";

        createDispoConfirmationRequest(externalOrderId);
        setLatestDeliveryDate(externalOrderId, LocalDate.now(ZoneId.of("Europe/Berlin")));

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(get("/api/customer/confirmations/{token}/driver-location", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationX").value(9.8820))
                .andExpect(jsonPath("$.locationY").value(49.8166));
    }

    @Test
    void confirm_returnsNoContentAndStoresCustomerResponse() throws Exception {
        String externalOrderId = "A-3002";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        CustomerResponseRequestDto request = new CustomerResponseRequestDto(
                CustomerResponseType.CONFIRM,
                "Bitte 30 Minuten vorher anrufen."
        );

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.CONFIRMED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        assertThat(confirmationRequest.isActive()).isFalse();

        boolean responseExists = customerResponseRepository
                .existsByConfirmationRequest(confirmationRequest);

        assertThat(responseExists).isTrue();

        var customerResponses = customerResponseRepository.findAll();

        assertThat(customerResponses).hasSize(1);
        assertThat(customerResponses.get(0).getComment())
                .isEqualTo("Bitte 30 Minuten vorher anrufen.");
    }

    @Test
    void confirm_whenFollowUpNotificationFails_stillStoresCustomerResponse() throws Exception {
        String externalOrderId = "A-3002-NOTIFICATION-FAILURE";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        Mockito.doThrow(new NotificationDeliveryException(
                        CommunicationChannel.EMAIL,
                        "Notification could not be delivered.",
                        new RuntimeException("Mail provider unavailable.")
                ))
                .when(notificationService)
                .sendCustomerResponseReceived(
                        any(Order.class),
                        any(ConfirmationRequest.class),
                        any(CustomerResponseType.class)
                );

        CustomerResponseRequestDto request = new CustomerResponseRequestDto(
                CustomerResponseType.CONFIRM,
                "Bitte 30 Minuten vorher anrufen."
        );

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(confirmationRequest.isActive()).isFalse();
        assertThat(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .isTrue();
    }

    @Test
    void reject_returnsNoContentAndStoresCustomerComment() throws Exception {
        String externalOrderId = "A-3003";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        CustomerResponseRequestDto request = new CustomerResponseRequestDto(
                CustomerResponseType.REJECT,
                "Bitte erst ab 15 Uhr."
        );

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.REJECTED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        assertThat(confirmationRequest.isActive()).isFalse();

        var customerResponses = customerResponseRepository.findAll();

        assertThat(customerResponses).hasSize(1);
        assertThat(customerResponses.get(0).getComment())
                .isEqualTo("Bitte erst ab 15 Uhr.");
    }

    @Test
    void confirm_returnsConflictWhenCustomerAnswersTwice() throws Exception {
        String externalOrderId = "A-3004";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"CONFIRM\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"CONFIRM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_INACTIVE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/customer/confirmations/" + token + "/response"));
    }

    @Test
    void getConfirmationPreview_returnsNotFoundForUnknownToken() throws Exception {
        mockMvc.perform(get("/api/customer/confirmations/{token}", "unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnGoneWhenCustomerAnswersExpiredRequest() throws Exception {
        String externalOrderId = "A-3006";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);
        expireRequest(token);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"CONFIRM\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_EXPIRED"))
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.path").value("/api/customer/confirmations/" + token + "/response"));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(customerResponseRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnConflictWhenCustomerAnswersInactiveRequest() throws Exception {
        String externalOrderId = "A-3007";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);
        markRequestInactive(token);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"REJECT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_INACTIVE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/customer/confirmations/" + token + "/response"));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);
        assertThat(customerResponseRepository.findAll()).isEmpty();
    }

    @Test
    void shouldAcceptCustomerAnswerWithoutComment() throws Exception {
        String externalOrderId = "A-3008";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"CONFIRM\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        var customerResponses = customerResponseRepository.findAll();

        assertThat(customerResponses).hasSize(1);
        assertThat(customerResponses.get(0).getComment()).isNull();
    }

    @Test
    void submitResponse_returnsValidationErrorWhenResponseTypeIsMissing() throws Exception {
        mockMvc.perform(post("/api/customer/confirmations/{token}/response", "any-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Response type is required."))
                .andExpect(jsonPath("$.status").value(400));
    }
    @Test
    void shouldReturnValidationErrorWhenCustomerCommentIsTooLong() throws Exception {
        String externalOrderId = "A-3009";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);
        String tooLongComment = "x".repeat(2001);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerResponseRequestDto(CustomerResponseType.REJECT, tooLongComment))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Customer comment must not exceed 2000 characters."))
                .andExpect(jsonPath("$.status").value(400));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(customerResponseRepository.findAll()).isEmpty();
    }

    private void createDispoConfirmationRequest(String externalOrderId) throws Exception {
        createDispoConfirmationRequest(externalOrderId, "2099-06-12");
    }

    private void createDispoConfirmationRequest(String externalOrderId, String deliveryDate) throws Exception {
        String requestJson = """
                {
                  "externalOrderId": "%s",
                  "tourNumber": "17",
                  "vehicleLicensePlate": "WÜ-AB 123",
                  "customerName": "Max Muller",
                  "communicationChannel": "EMAIL",
                  "customerEmail": "daniel@example.com",
                  "customerPhoneNumber": "+491701234567",
                  "deliveryAddress": "Beispielstrase 12, 97070 Wurzburg",
                  "locationX": 9.8820,
                  "locationY": 49.8166,
                  "targetLocationX": 9.9372,
                  "targetLocationY": 49.7935,
                  "product": "Heizol",
                  "quantityLiters": 3000,
                  "deliveryDate": "%s",
                  "deliveryWindowStart": "10:00",
                  "deliveryWindowEnd": "11:00",
                  "responseDeadlineHours": 24,
                  "priceDisplayText": "100 EUR"
                }
                """.formatted(externalOrderId, deliveryDate);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .header("X-API-Key", TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId))
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findTopByOrderOrderByIdDesc(order)
                .orElseThrow();
        confirmationRequest.markSent(Instant.now(clock));
        order.markSent();
        confirmationRequestRepository.save(confirmationRequest);
        orderRepository.save(order);
    }

    private String findActiveTokenByExternalOrderId(String externalOrderId) {
        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        return confirmationRequestRepository
                .findTopByOrderOrderByIdDesc(order)
                .orElseThrow()
                .getToken();
    }

    private String createLaterRequest(String externalOrderId) {
        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();
        ConfirmationRequest request = ConfirmationRequest.createPending(
                order,
                UUID.randomUUID().toString(),
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2099, 6, 12),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                24
        );
        request.markSent(Instant.now(clock));
        confirmationRequestRepository.saveAndFlush(request);
        entityManager.clear();
        return request.getToken();
    }

    private void setLatestDeliveryDate(String externalOrderId, LocalDate deliveryDate) {
        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(1L, externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findTopByOrderOrderByIdDesc(order)
                .orElseThrow();

        jdbcTemplate.update(
                "update confirmation_request set delivery_date = ? where id = ?",
                deliveryDate,
                confirmationRequest.getId()
        );
        entityManager.clear();
    }

    private void expireRequest(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        jdbcTemplate.update(
                "update confirmation_request set expires_at = ? where id = ?",
                Timestamp.from(Instant.now(clock).minusSeconds(1)),
                confirmationRequest.getId()
        );
        entityManager.clear();
    }

    private void markRequestInactive(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findLatestByToken(token)
                .orElseThrow();

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);
    }
}

