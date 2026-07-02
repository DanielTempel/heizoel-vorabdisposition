package heizoel.backend.confirmation.adapter.web.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.confirmation.adapter.web.customer.dto.CustomerResponseRequestDto;
import heizoel.backend.confirmation.adapter.persistence.CustomerResponseRepository;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.adapter.persistence.ConfirmationRequestRepository;
import heizoel.backend.confirmation.adapter.persistence.OrderSnapshotRepository;
import heizoel.backend.confirmation.application.port.out.location.GeocodingClient;
import heizoel.backend.confirmation.application.port.out.location.LocationTrackingService;
import heizoel.backend.confirmation.domain.model.GeoCoordinate;
import heizoel.backend.confirmation.application.port.out.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerConfirmationIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

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
    JavaMailSender javaMailSender;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    LocationTrackingService locationTrackingService;

    @MockitoBean
    GeocodingClient geocodingClient;

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

    @BeforeEach
    void cleanDatabase() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderSnapshotRepository.deleteAll();

        Mockito.reset(notificationService, locationTrackingService, geocodingClient);
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

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.CONFIRMED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
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

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.REJECTED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
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

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
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

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
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

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(customerResponseRepository.findAll()).isEmpty();
    }

    @Test
    void createDispoConfirmationRequest_usesNotificationService() throws Exception {
        String externalOrderId = "A-3005";

        createDispoConfirmationRequest(externalOrderId);

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(
                        any(OrderSnapshot.class),
                        any(ConfirmationRequest.class)
                );
    }

    private void createDispoConfirmationRequest(String externalOrderId) throws Exception {
        createDispoConfirmationRequest(externalOrderId, "2099-06-12");
    }

    private void createDispoConfirmationRequest(String externalOrderId, String deliveryDate) throws Exception {
        String requestJson = """
                {
                  "externalOrderId": "%s",
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));
    }

    private String findActiveTokenByExternalOrderId(String externalOrderId) {
        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        return confirmationRequestRepository
                .findTopByOrderSnapshotOrderByIdDesc(orderSnapshot)
                .orElseThrow()
                .getToken();
    }

    private void setLatestDeliveryDate(String externalOrderId, LocalDate deliveryDate) {
        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findTopByOrderSnapshotOrderByIdDesc(orderSnapshot)
                .orElseThrow();

        jdbcTemplate.update(
                "update confirmation_request set delivery_date = ? where id = ?",
                deliveryDate,
                confirmationRequest.getId()
        );
    }

    private void expireRequest(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        jdbcTemplate.update(
                "update confirmation_request set expires_at = ? where id = ?",
                Instant.now().minusSeconds(1),
                confirmationRequest.getId()
        );
    }

    private void markRequestInactive(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        confirmationRequest.markInactive();
        ((heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort)
                confirmationRequestRepository).save(confirmationRequest);
    }
}

