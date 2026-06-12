package heizoel.backend.dispo;

import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;
import heizoel.backend.dispo.application.interfaces.DispoStatusCallbackService;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.repository.ConfirmationRequestRepository;
import heizoel.backend.dispo.domain.repository.OrderSnapshotRepository;
import heizoel.backend.notification.application.interfaces.NotificationService;
import heizoel.backend.notification.domain.CommunicationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DispoCallbackIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
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
    private MockMvc mockMvc;

    @Autowired
    private OrderSnapshotRepository orderSnapshotRepository;

    @Autowired
    private ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    private CustomerResponseRepository customerResponseRepository;

    @MockitoBean
    private DispoStatusCallbackService dispoStatusCallbackService;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderSnapshotRepository.deleteAll();

        reset(dispoStatusCallbackService, notificationService);
    }

    @Test
    void confirm_shouldSendDispoCallbackAfterCommit() throws Exception {
        String externalOrderId = "A-CB-1001";

        createEmailDispoConfirmationRequest(
                externalOrderId,
                "10:00",
                "11:00"
        );

        String token = findActiveToken(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerComment": "Bitte 30 Minuten vorher anrufen."
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(dispoStatusCallbackService, timeout(1000)).sendStatusUpdate(argThat(
                statusUpdateMatches(
                        externalOrderId,
                        ConfirmationStatus.CONFIRMED,
                        "Bitte 30 Minuten vorher anrufen."
                )
        ));
    }

    @Test
    void reject_shouldSendDispoCallbackAfterCommit() throws Exception {
        String externalOrderId = "A-CB-1002";

        createEmailDispoConfirmationRequest(
                externalOrderId,
                "12:00",
                "13:00"
        );

        String token = findActiveToken(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/reject", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerComment": "Bitte erst ab 15 Uhr."
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(dispoStatusCallbackService, timeout(1000)).sendStatusUpdate(argThat(
                statusUpdateMatches(
                        externalOrderId,
                        ConfirmationStatus.REJECTED,
                        "Bitte erst ab 15 Uhr."
                )
        ));
    }

    @Test
    void callbackFailure_shouldNotRollbackCustomerConfirmation() throws Exception {
        String externalOrderId = "A-CB-1003";

        createEmailDispoConfirmationRequest(
                externalOrderId,
                "14:00",
                "15:00"
        );

        String token = findActiveToken(externalOrderId);

        doThrow(new RuntimeException("DISPO unavailable"))
                .when(dispoStatusCallbackService)
                .sendStatusUpdate(any(DispoConfirmationStatusUpdateDto.class));

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerComment": "Test comment"
                                }
                                """))
                .andExpect(status().isNoContent());

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        assertThat(orderSnapshot.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.CONFIRMED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        assertThat(confirmationRequest.isActive())
                .isFalse();

        assertThat(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .isTrue();

        verify(dispoStatusCallbackService, timeout(1000))
                .sendStatusUpdate(any(DispoConfirmationStatusUpdateDto.class));
    }

    @Test
    void confirm_smsRequest_shouldStillSendDispoCallback() throws Exception {
        String externalOrderId = "A-CB-SMS-1004";

        createSmsDispoConfirmationRequest(
                externalOrderId
        );

        String token = findActiveToken(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerComment": "SMS confirmation works."
                                }
                                """))
                .andExpect(status().isNoContent());

        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        assertThat(orderSnapshot.getCustomerEmail()).isNull();
        assertThat(orderSnapshot.getCustomerPhoneNumber()).isEqualTo("+491701234567");
        assertThat(orderSnapshot.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findByToken(token)
                .orElseThrow();

        assertThat(confirmationRequest.getCommunicationChannel())
                .isEqualTo(CommunicationChannel.SMS);

        verify(dispoStatusCallbackService, timeout(1000)).sendStatusUpdate(argThat(
                statusUpdateMatches(
                        externalOrderId,
                        ConfirmationStatus.CONFIRMED,
                        "SMS confirmation works."
                )
        ));
    }

    private void createEmailDispoConfirmationRequest(
            String externalOrderId,
            String deliveryWindowStart,
            String deliveryWindowEnd
    ) throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId": "%s",
                                  "customerName": "Max Muller",
                                  "customerEmail": "daniel@example.com",
                                  "customerPhoneNumber": null,
                                  "communicationChannel": "EMAIL",
                                  "deliveryAddress": "Beispielstrase 12, 97070 Wurzburg",
                                  "product": "Heizol",
                                  "quantityLiters": 3000,
                                  "deliveryDate": "2026-06-12",
                                  "deliveryWindowStart": "%s",
                                  "deliveryWindowEnd": "%s"
                                }
                                """.formatted(
                                externalOrderId,
                                deliveryWindowStart,
                                deliveryWindowEnd
                        )))
                .andExpect(status().isCreated());
    }

    private void createSmsDispoConfirmationRequest(
            String externalOrderId
    ) throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId": "%s",
                                  "customerName": "Max Muller",
                                  "customerEmail": null,
                                  "customerPhoneNumber": "+491701234567",
                                  "communicationChannel": "SMS",
                                  "deliveryAddress": "Beispielstrase 12, 97070 Wurzburg",
                                  "product": "Heizol",
                                  "quantityLiters": 3000,
                                  "deliveryDate": "2026-06-12",
                                  "deliveryWindowStart": "%s",
                                  "deliveryWindowEnd": "%s"
                                }
                                """.formatted(
                                externalOrderId,
                                "16:00",
                                "17:00"
                        )))
                .andExpect(status().isCreated());
    }

    private String findActiveToken(String externalOrderId) {
        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        ConfirmationRequest confirmationRequest = confirmationRequestRepository
                .findTopByOrderSnapshotOrderByIdDesc(orderSnapshot)
                .orElseThrow();

        return confirmationRequest.getToken();
    }

    private ArgumentMatcher<DispoConfirmationStatusUpdateDto> statusUpdateMatches(
            String externalOrderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    ) {
        return update ->
                update != null
                        && externalOrderId.equals(update.externalOrderId())
                        && confirmationStatus == update.confirmationStatus()
                        && customerComment.equals(update.customerComment());
    }
}