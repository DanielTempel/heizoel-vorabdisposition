package heizoel.backend.dispo;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.repository.ConfirmationRequestRepository;
import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
import heizoel.backend.dispo.domain.repository.OrderSnapshotRepository;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

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
    ConfirmationNotificationService notificationService;

    @BeforeEach
    void cleanDatabase() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderSnapshotRepository.deleteAll();

        Mockito.reset(notificationService);
    }

    @Test
    void createConfirmationRequest_validRequest_createsOrderSnapshotAndConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("10:00", "11:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);

        OrderSnapshot orderSnapshot = orderSnapshots.get(0);
        ConfirmationRequest confirmationRequest = confirmationRequests.get(0);

        assertThat(orderSnapshot.getExternalOrderId()).isEqualTo("A-1024");
        assertThat(orderSnapshot.getCustomerName()).isEqualTo("Max Muller");
        assertThat(orderSnapshot.getCustomerEmail()).isEqualTo("daniel@example.com");
        assertThat(orderSnapshot.getDeliveryAddress()).isEqualTo("Beispielstrase 12, 97070 Wurzburg");
        assertThat(orderSnapshot.getProduct()).isEqualTo("Heizol");
        assertThat(orderSnapshot.getQuantityLiters()).isEqualTo(3000);
        assertThat(orderSnapshot.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        assertThat(confirmationRequest.getOrderSnapshot().getId()).isEqualTo(orderSnapshot.getId());
        assertThat(confirmationRequest.getToken()).isNotBlank();
        assertThat(confirmationRequest.getDeliveryDate()).hasToString("2026-06-12");
        assertThat(confirmationRequest.getDeliveryWindowStart()).hasToString("10:00");
        assertThat(confirmationRequest.getDeliveryWindowEnd()).hasToString("11:00");
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(confirmationRequest.getSentAt()).isNotNull();
        assertThat(confirmationRequest.getExpiresAt()).isAfter(confirmationRequest.getSentAt());

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequestEmail(any(OrderSnapshot.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_duplicateUnchangedRequest_returnsOkAndDoesNotCreateSecondConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("10:00", "11:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("10:00", "11:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);
        assertThat(confirmationRequests.get(0).isActive()).isTrue();

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequestEmail(any(OrderSnapshot.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_changedDeliveryWindow_invalidatesOldRequestAndCreatesNewRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("10:00", "11:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("13:00", "14:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
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

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequestEmail(any(OrderSnapshot.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_invalidDeliveryWindow_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("14:00", "13:00")))
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
    void createConfirmationRequest_missingCustomerEmail_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithCustomerEmail()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Customer e-mail is required for digital confirmation."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/dispo/confirmation-requests"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(orderSnapshotRepository.findAll()).isEmpty();
        assertThat(confirmationRequestRepository.findAll()).isEmpty();

        Mockito.verifyNoInteractions(notificationService);
    }

    private String validRequest(String deliveryWindowStart, String deliveryWindowEnd) throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                "A-1024",
                "Max Muller",
                "daniel@example.com",
                "Beispielstrase 12, 97070 Wurzburg",
                "Heizol",
                3000,
                "2026-06-12",
                deliveryWindowStart,
                deliveryWindowEnd
        ));
    }

    private String requestWithCustomerEmail() throws Exception {
        return objectMapper.writeValueAsString(new TestDispoRequest(
                "A-1024",
                "Max Muller",
                "",
                "Beispielstrase 12, 97070 Wurzburg",
                "Heizol",
                3000,
                "2026-06-12",
                "10:00",
                "11:00"
        ));
    }

    private record TestDispoRequest(
            String externalOrderId,
            String customerName,
            String customerEmail,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String deliveryDate,
            String deliveryWindowStart,
            String deliveryWindowEnd
    ) {
    }
}