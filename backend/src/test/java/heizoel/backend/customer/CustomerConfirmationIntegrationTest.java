package heizoel.backend.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.customer.api.dto.CustomerAnswerRequestDto;
import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
import heizoel.backend.dispo.api.dto.request.DispoConfirmationRequestDto;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.repository.ConfirmationRequestRepository;
import heizoel.backend.dispo.domain.repository.OrderSnapshotRepository;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import heizoel.backend.notification.domain.CommunicationChannel;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerConfirmationIntegrationTest {

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

        registry.add("heizoel.confirmation.response-deadline", () -> "PT24H");
        registry.add("heizoel.confirmation.frontend-url", () -> "http://localhost:3000");
        registry.add("heizoel.confirmation.dispo-url", () -> "http://localhost:8090/api/dispo/confirmation-status-updates");
    }

    @MockitoBean
    JavaMailSender javaMailSender;

    @MockitoBean
    ConfirmationNotificationService confirmationNotificationService;

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

        Mockito.reset(confirmationNotificationService);
    }

    @Test
    void shouldReturnConfirmationPreviewByToken() throws Exception {
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
                .andExpect(jsonPath("$.deliveryDate").value("2026-06-12"))
                .andExpect(jsonPath("$.deliveryWindowStart").value("10:00:00"))
                .andExpect(jsonPath("$.deliveryWindowEnd").value("11:00:00"));
    }

    @Test
    void shouldConfirmDeliveryWindowAndStoreCustomerResponse() throws Exception {
        String externalOrderId = "A-3002";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        CustomerAnswerRequestDto request = new CustomerAnswerRequestDto(
                "Bitte 30 Minuten vorher anrufen."
        );

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token)
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
    void shouldRejectDeliveryWindowAndStoreCustomerComment() throws Exception {
        String externalOrderId = "A-3003";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        CustomerAnswerRequestDto request = new CustomerAnswerRequestDto(
                "Bitte erst ab 15 Uhr."
        );

        mockMvc.perform(post("/api/customer/confirmations/{token}/reject", token)
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
    void shouldReturnConflictWhenCustomerAnswersTwice() throws Exception {
        String externalOrderId = "A-3004";

        createDispoConfirmationRequest(externalOrderId);

        String token = findActiveTokenByExternalOrderId(externalOrderId);

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/customer/confirmations/{token}/confirm", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_INACTIVE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/customer/confirmations/" + token + "/confirm"));
    }

    @Test
    void shouldReturnNotFoundForUnknownToken() throws Exception {
        mockMvc.perform(get("/api/customer/confirmations/{token}", "unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createDispoConfirmationRequest_shouldUseNotificationService() throws Exception {
        String externalOrderId = "A-3005";

        createDispoConfirmationRequest(externalOrderId);

        Mockito.verify(confirmationNotificationService, times(1))
                .sendConfirmationRequest(
                        any(OrderSnapshot.class),
                        any(ConfirmationRequest.class)
                );
    }

    private void createDispoConfirmationRequest(String externalOrderId) throws Exception {
        DispoConfirmationRequestDto request = new DispoConfirmationRequestDto(
                externalOrderId,
                "Max Muller",
                CommunicationChannel.EMAIL,
                "daniel@example.com",
                "+491701234567",
                "Beispielstrase 12, 97070 Wurzburg",
                "Heizol",
                3000,
                LocalDate.of(2026, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));
    }

    private String findActiveTokenByExternalOrderId(String externalOrderId) {
        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();

        return confirmationRequestRepository
                .findByOrderSnapshotAndActiveTrue(orderSnapshot)
                .orElseThrow()
                .getToken();
    }
}