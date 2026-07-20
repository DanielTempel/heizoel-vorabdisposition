package heizoel.backend.adapter.out.workflow;

import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.application.port.out.location.GeocodingClient;
import heizoel.backend.application.model.GeoCoordinate;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.domain.CommunicationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ConfirmationFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("camunda.bpm.auto-deployment-enabled", () -> "true");
        registry.add("camunda.bpm.deployment-resource-pattern[0]", () -> "classpath*:processes/*.bpmn");
        registry.add("camunda.bpm.job-execution.enabled", () -> "true");

        registry.add("heizoel.confirmation.frontend-url", () -> "http://localhost:3000");
        registry.add("heizoel.confirmation.dispo-url", () -> "http://localhost:8090/api/dispo/confirmation-status-updates");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    HandleNoResponseTimeoutUseCase handleNoResponseTimeoutUseCase;

    @MockitoBean
    DispoStatusCallbackService dispoStatusCallbackService;
    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @MockitoBean
    GeocodingClient geocodingClient;

    @BeforeEach
    void resetMocks() {
        reset(dispoStatusCallbackService, notificationService, geocodingClient);
        when(geocodingClient.geocode(anyString()))
                .thenReturn(java.util.Optional.of(new GeoCoordinate(9.9372D, 49.7935D)));
    }

    @Test
    void shouldScheduleTimeoutJobAtPersistedExpiration() throws Exception {
        String externalOrderId = uniqueOrderId("A-CAMUNDA-ABSOLUTE-DEADLINE");

        createDispoConfirmationRequest(externalOrderId)
                .andExpect(status().isCreated());

        Timestamp expiresAt = jdbcTemplate.queryForObject("""
                SELECT cr.expires_at
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, Timestamp.class, externalOrderId);

        Timestamp jobDueDate = jdbcTemplate.queryForObject("""
                SELECT j.duedate_
                FROM act_ru_job j
                JOIN act_ru_variable external_order
                  ON external_order.proc_inst_id_ = j.process_instance_id_
                WHERE external_order.name_ = 'confirmationRequestId'
                  AND external_order.long_ = (
                      SELECT cr.id
                      FROM confirmation_request cr
                      JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                      WHERE os.external_order_id = ?
                      ORDER BY cr.id DESC
                      LIMIT 1
                  )
                """, Timestamp.class, externalOrderId);

        long dueDateDifferenceMillis = Math.abs(
                jobDueDate.toInstant().toEpochMilli() - expiresAt.toInstant().toEpochMilli()
        );
        assertThat(dueDateDifferenceMillis).isLessThanOrEqualTo(1_000L);
    }

    @Test
    void shouldSetNoResponseWhenCustomerDoesNotAnswer() throws Exception {
        String externalOrderId = uniqueOrderId("A-CAMUNDA-NO-RESPONSE");

        createDispoConfirmationRequest(externalOrderId)
                .andExpect(status().isCreated());
        expireLatestConfirmationRequest(externalOrderId);

        await()
                .atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    assertThat(getConfirmationStatus(externalOrderId))
                            .isEqualTo("NO_RESPONSE");

                    assertThat(isActiveRequest(externalOrderId))
                            .isFalse();
                });

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        verify(dispoStatusCallbackService, atLeastOnce())
                                .sendStatusUpdate(argThat(statusUpdateFor(
                                        externalOrderId,
                                        ConfirmationStatus.NO_RESPONSE
                                )))
                );
    }

    @Test
    void shouldNotOverwriteConfirmedStatusWhenCustomerAnsweredBeforeTimeout() throws Exception {
        String externalOrderId = uniqueOrderId("A-CAMUNDA-CONFIRMED");

        createDispoConfirmationRequest(externalOrderId)
                .andExpect(status().isCreated());

        String token = getLatestToken(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseType\":\"CONFIRM\"}"))
                .andExpect(status().isNoContent());

        assertThat(getConfirmationStatus(externalOrderId))
                .isEqualTo("CONFIRMED");

        assertThat(isActiveRequest(externalOrderId))
                .isFalse();

        expireLatestConfirmationRequest(externalOrderId);

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        verify(dispoStatusCallbackService, atLeastOnce())
                                .sendStatusUpdate(argThat(statusUpdateFor(
                                        externalOrderId,
                                        ConfirmationStatus.CONFIRMED
                                )))
                );

        await()
                .pollDelay(Duration.ofSeconds(8))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(getConfirmationStatus(externalOrderId))
                                .isEqualTo("CONFIRMED")
                );

        verify(dispoStatusCallbackService, never())
                .sendStatusUpdate(argThat(statusUpdateFor(
                        externalOrderId,
                        ConfirmationStatus.NO_RESPONSE
                )));
    }

    @Test
    void shouldStoreRejectedCustomerResponse() throws Exception {
        String externalOrderId = uniqueOrderId("A-CUSTOMER-REJECTED");

        createDispoConfirmationRequest(externalOrderId)
                .andExpect(status().isCreated());

        String token = getLatestToken(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/response", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "responseType": "REJECT",
                                  "customerComment": "Bitte erst ab 15 Uhr."
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(getConfirmationStatus(externalOrderId))
                .isEqualTo("REJECTED");

        assertThat(isActiveRequest(externalOrderId))
                .isFalse();

        assertThat(getLatestCustomerResponseType(externalOrderId))
                .isEqualTo("REJECT");

        assertThat(getLatestCustomerComment(externalOrderId))
                .isEqualTo("Bitte erst ab 15 Uhr.");

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        verify(dispoStatusCallbackService, atLeastOnce())
                                .sendStatusUpdate(argThat(statusUpdateFor(
                                        externalOrderId,
                                        ConfirmationStatus.REJECTED
                                )))
                );
    }

    private org.springframework.test.web.servlet.ResultActions createDispoConfirmationRequest(
            String externalOrderId
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "externalOrderId": "%s",
                          "customerName": "Max Muller",
                          "communicationChannel": "%s",
                          "customerEmail": "daniel@example.com",
                          "customerPhoneNumber": null,
                          "deliveryAddress": "Beispielstrase 12, 97070 Wurzburg",
                          "locationX": 9.8820,
                          "locationY": 49.8166,
                          "targetLocationX": 9.9372,
                          "targetLocationY": 49.7935,
                          "product": "Heizol",
                          "quantityLiters": 3000,
                          "deliveryDate": "2099-06-12",
                          "deliveryWindowStart": "10:00",
                          "deliveryWindowEnd": "11:00",
                          "responseDeadlineHours": 24,
                          "priceDisplayText": "100 EUR"
                        }
                        """.formatted(
                        externalOrderId,
                        CommunicationChannel.EMAIL.name()
                )));
    }

    private String uniqueOrderId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String getLatestToken(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT cr.token
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, String.class, externalOrderId);
    }

    private void expireLatestConfirmationRequest(String externalOrderId) {
        Long confirmationRequestId = jdbcTemplate.queryForObject("""
                SELECT cr.id
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, Long.class, externalOrderId);

        jdbcTemplate.update("""
                UPDATE confirmation_request
                SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """, confirmationRequestId);

        handleNoResponseTimeoutUseCase.handleTimeout(confirmationRequestId);
    }

    private String getConfirmationStatus(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT confirmation_status
                FROM order_snapshot
                WHERE external_order_id = ?
                """, String.class, externalOrderId);
    }

    private boolean isActiveRequest(String externalOrderId) {
        Boolean active = jdbcTemplate.queryForObject("""
                SELECT cr.active
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, Boolean.class, externalOrderId);

        return Boolean.TRUE.equals(active);
    }

    private String getLatestCustomerResponseType(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT resp.response_type
                FROM customer_response resp
                JOIN confirmation_request cr ON cr.id = resp.confirmation_request_id
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY resp.id DESC
                LIMIT 1
                """, String.class, externalOrderId);
    }

    private String getLatestCustomerComment(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT resp.comment
                FROM customer_response resp
                JOIN confirmation_request cr ON cr.id = resp.confirmation_request_id
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY resp.id DESC
                LIMIT 1
                """, String.class, externalOrderId);
    }

    private ArgumentMatcher<DispoStatusCallbackRequest>
    statusUpdateFor(String externalOrderId, ConfirmationStatus confirmationStatus) {
        return update ->
                update != null
                        && externalOrderId.equals(update.externalOrderId())
                        && confirmationStatus == update.confirmationStatus();
    }
}

