package heizoel.backend.adapter.out.notification;

import heizoel.backend.adapter.out.notification.twilio.TwilioMessageSender;
import heizoel.backend.application.model.GeoCoordinate;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestResult;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestUseCase;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.application.port.out.location.GeocodingClient;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.domain.CommunicationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class NotificationDeliveryErrorIntegrationTest {

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
        registry.add("camunda.bpm.job-execution.enabled", () -> "false");

        registry.add("heizoel.confirmation.frontend-url", () -> "http://localhost:3000");
        registry.add("heizoel.mail.from", () -> "no-reply@heizoel.local");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SendConfirmationRequestUseCase sendConfirmationRequestUseCase;

    @MockitoBean
    TwilioMessageSender twilioMessageSender;

    @MockitoBean
    JavaMailSender javaMailSender;

    @MockitoBean
    DispoStatusCallbackService dispoStatusCallbackService;

    @MockitoBean
    GeocodingClient geocodingClient;

    @BeforeEach
    void resetMocks() {
        reset(twilioMessageSender, geocodingClient);
        when(geocodingClient.geocode(anyString()))
                .thenReturn(java.util.Optional.of(new GeoCoordinate(9.9372D, 49.7935D)));
    }

    @Test
    void createConfirmationRequestReturnsBadGatewayWhenSmsDeliveryFails() throws Exception {
        doThrow(new NotificationDeliveryException(
                CommunicationChannel.SMS,
                "Twilio notification could not be delivered for externalOrderId=A-SMS-FAIL-1",
                new RuntimeException("boom")
        )).when(twilioMessageSender).sendSms(anyString(), anyString(), any());

        createDispoConfirmationRequest("A-SMS-FAIL-1", "SMS")
                .andExpect(status().isAccepted());

        Long confirmationRequestId = findLatestConfirmationRequestId("A-SMS-FAIL-1");

        SendConfirmationRequestResult result = sendConfirmationRequestUseCase.send(confirmationRequestId);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.RETRYABLE_FAILURE);
        assertThat(getDeliveryStatus("A-SMS-FAIL-1")).isEqualTo("PENDING");
        assertThat(getOrderStatus("A-SMS-FAIL-1")).isEqualTo("OPEN");
    }

    @Test
    void createConfirmationRequestReturnsBadGatewayWhenWhatsAppDeliveryFails() throws Exception {
        doThrow(new NotificationDeliveryException(
                CommunicationChannel.WHATSAPP,
                "Twilio notification could not be delivered for externalOrderId=A-WA-FAIL-1",
                new RuntimeException("boom")
        )).when(twilioMessageSender).sendWhatsApp(anyString(), anyString(), any());

        createDispoConfirmationRequest("A-WA-FAIL-1", "WHATSAPP")
                .andExpect(status().isAccepted());

        Long confirmationRequestId = findLatestConfirmationRequestId("A-WA-FAIL-1");

        SendConfirmationRequestResult result = sendConfirmationRequestUseCase.send(confirmationRequestId);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.RETRYABLE_FAILURE);
        assertThat(getDeliveryStatus("A-WA-FAIL-1")).isEqualTo("PENDING");
        assertThat(getOrderStatus("A-WA-FAIL-1")).isEqualTo("OPEN");
    }

    private org.springframework.test.web.servlet.ResultActions createDispoConfirmationRequest(
            String externalOrderId,
            String communicationChannel
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "externalOrderId": "%s",
                          "tourNumber": "17",
                          "vehicleLicensePlate": "WÜ-AB 123",
                          "customerName": "Max Muller",
                          "customerEmail": null,
                          "customerPhoneNumber": "+491701234567",
                          "communicationChannel": "%s",
                          "deliveryAddress": "Beispielstrasse 12, 97070 Wuerzburg",
                          "locationX": 9.8820,
                          "locationY": 49.8166,
                          "targetLocationX": 9.9372,
                          "targetLocationY": 49.7935,
                          "product": "Heizoel",
                          "quantityLiters": 3000,
                          "deliveryDate": "2099-06-12",
                          "deliveryWindowStart": "10:00",
                          "deliveryWindowEnd": "11:00",
                          "responseDeadlineHours": 24,
                          "priceDisplayText": "100 EUR"
                        }
                        """.formatted(externalOrderId, communicationChannel)));
    }

    private Long findLatestConfirmationRequestId(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT cr.id
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, Long.class, externalOrderId);
    }

    private String getDeliveryStatus(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT cr.delivery_status
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, String.class, externalOrderId);
    }

    private String getOrderStatus(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT confirmation_status
                FROM order_snapshot
                WHERE external_order_id = ?
                """, String.class, externalOrderId);
    }
}
