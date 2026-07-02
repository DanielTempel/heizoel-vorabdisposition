package heizoel.backend.confirmation.adapter.out.notification;

import heizoel.backend.confirmation.application.port.out.ConfirmationWorkflowService;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackService;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.application.port.out.GeocodingClient;
import heizoel.backend.confirmation.domain.model.GeoCoordinate;
import heizoel.backend.confirmation.application.port.out.EmailSender;
import heizoel.backend.confirmation.application.port.out.SmsConfirmationSender;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ConfirmationNotificationTest {
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

        /*
         * We mock ConfirmationWorkflowService in this test.
         * Therefore, the Camunda job executor is not needed here.
         */
        registry.add("camunda.bpm.job-execution.enabled", () -> "false");

        registry.add("heizoel.confirmation.frontend-url", () -> "http://localhost:3000");
        registry.add("heizoel.confirmation.dispo-url", () -> "http://localhost:8090/api/dispo/confirmation-status-updates");

        registry.add("heizoel.mail.from", () -> "no-reply@heizoel.local");
        registry.add("heizoel.sms.mock-url", () -> "http://localhost:8091/api/sms/messages");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    EmailSender emailSender;

    @MockitoBean
    SmsConfirmationSender smsConfirmationSender;

    @MockitoBean
    ConfirmationWorkflowService confirmationWorkflowService;

    @MockitoBean
    DispoStatusCallbackService dispoStatusCallbackService;

    @MockitoBean
    GeocodingClient geocodingClient;

    @BeforeEach
    void resetMocks() {
        reset(
                emailSender,
                smsConfirmationSender,
                confirmationWorkflowService,
                dispoStatusCallbackService,
                geocodingClient
        );
        when(geocodingClient.geocode(anyString()))
                .thenReturn(java.util.Optional.of(new GeoCoordinate(9.9372D, 49.7935D)));
    }

    @Test
    void shouldSendConfirmationViaEmail_whenCommunicationChannelIsEmail() throws Exception {
        String externalOrderId = uniqueOrderId("A-NOTIFICATION-EMAIL");

        createDispoConfirmationRequest(
                externalOrderId,
                "EMAIL",
                "daniel@example.com",
                null
        ).andExpect(status().isCreated());

        assertThat(getConfirmationStatus(externalOrderId))
                .isEqualTo("SENT");

        assertThat(getLatestCommunicationChannel(externalOrderId))
                .isEqualTo("EMAIL");

        assertThat(getCustomerEmail(externalOrderId))
                .isEqualTo("daniel@example.com");

        assertThat(getCustomerPhoneNumber(externalOrderId))
                .isNull();

        ArgumentCaptor<OrderSnapshot> orderCaptor =
                ArgumentCaptor.forClass(OrderSnapshot.class);

        ArgumentCaptor<ConfirmationRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfirmationRequest.class);

        verify(emailSender, times(1))
                .sendConfirmationRequest(orderCaptor.capture(), requestCaptor.capture());

        verifyNoInteractions(smsConfirmationSender);

        OrderSnapshot capturedOrder = orderCaptor.getValue();
        ConfirmationRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedOrder.getExternalOrderId())
                .isEqualTo(externalOrderId);

        assertThat(capturedOrder.getCustomerEmail())
                .isEqualTo("daniel@example.com");

        assertThat(capturedRequest.getCommunicationChannel())
                .isEqualTo(CommunicationChannel.EMAIL);

        assertThat(capturedRequest.getToken())
                .isNotBlank();

        verify(confirmationWorkflowService, times(1))
                .startTimeoutProcess(any(ConfirmationRequest.class));
    }

    @Test
    void shouldSendConfirmationViaSms_whenCommunicationChannelIsSms() throws Exception {
        String externalOrderId = uniqueOrderId("A-NOTIFICATION-SMS");

        createDispoConfirmationRequest(
                externalOrderId,
                "SMS",
                null,
                "+491701234567"
        ).andExpect(status().isCreated());

        assertThat(getConfirmationStatus(externalOrderId))
                .isEqualTo("SENT");

        assertThat(getLatestCommunicationChannel(externalOrderId))
                .isEqualTo("SMS");

        assertThat(getCustomerEmail(externalOrderId))
                .isNull();

        assertThat(getCustomerPhoneNumber(externalOrderId))
                .isEqualTo("+491701234567");

        ArgumentCaptor<OrderSnapshot> orderCaptor =
                ArgumentCaptor.forClass(OrderSnapshot.class);

        ArgumentCaptor<ConfirmationRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfirmationRequest.class);

        verify(smsConfirmationSender, times(1))
                .send(orderCaptor.capture(), requestCaptor.capture());

        verifyNoInteractions(emailSender);

        OrderSnapshot capturedOrder = orderCaptor.getValue();
        ConfirmationRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedOrder.getExternalOrderId())
                .isEqualTo(externalOrderId);

        assertThat(capturedOrder.getCustomerPhoneNumber())
                .isEqualTo("+491701234567");

        assertThat(capturedRequest.getCommunicationChannel())
                .isEqualTo(CommunicationChannel.SMS);

        assertThat(capturedRequest.getToken())
                .isNotBlank();

        verify(confirmationWorkflowService, times(1))
                .startTimeoutProcess(any(ConfirmationRequest.class));
    }

    private org.springframework.test.web.servlet.ResultActions createDispoConfirmationRequest(
            String externalOrderId,
            String communicationChannel,
            String customerEmail,
            String customerPhoneNumber
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "externalOrderId": "%s",
                          "customerName": "Max Muller",
                          "customerEmail": %s,
                          "customerPhoneNumber": %s,
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
                        """.formatted(
                        externalOrderId,
                        jsonStringOrNull(customerEmail),
                        jsonStringOrNull(customerPhoneNumber),
                        communicationChannel
                )));
    }

    private String jsonStringOrNull(String value) {
        if (value == null) {
            return "null";
        }

        return "\"" + value + "\"";
    }

    private String uniqueOrderId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String getConfirmationStatus(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT confirmation_status
                FROM order_snapshot
                WHERE external_order_id = ?
                """, String.class, externalOrderId);
    }

    private String getCustomerEmail(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT customer_email
                FROM order_snapshot
                WHERE external_order_id = ?
                """, String.class, externalOrderId);
    }

    private String getCustomerPhoneNumber(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT customer_phone_number
                FROM order_snapshot
                WHERE external_order_id = ?
                """, String.class, externalOrderId);
    }

    private String getLatestCommunicationChannel(String externalOrderId) {
        return jdbcTemplate.queryForObject("""
                SELECT cr.communication_channel
                FROM confirmation_request cr
                JOIN order_snapshot os ON os.id = cr.order_snapshot_id
                WHERE os.external_order_id = ?
                ORDER BY cr.id DESC
                LIMIT 1
                """, String.class, externalOrderId);
    }
}

