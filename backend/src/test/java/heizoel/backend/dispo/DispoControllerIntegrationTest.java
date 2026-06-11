package heizoel.backend.dispo;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
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
    ConfirmationNotificationService notificationService;

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

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);

        OrderSnapshot orderSnapshot = orderSnapshots.get(0);
        ConfirmationRequest confirmationRequest = confirmationRequests.get(0);

        assertThat(orderSnapshot.getExternalOrderId()).isEqualTo("A-1024");
        assertThat(orderSnapshot.getCustomerName()).isEqualTo("Max Muller");
        assertThat(orderSnapshot.getCustomerEmail()).isEqualTo("daniel@example.com");
        assertThat(orderSnapshot.getCustomerPhoneNumber()).isNull();
        assertThat(orderSnapshot.getDeliveryAddress()).isEqualTo("Beispielstrase 12, 97070 Wurzburg");
        assertThat(orderSnapshot.getProduct()).isEqualTo("Heizol");
        assertThat(orderSnapshot.getQuantityLiters()).isEqualTo(3000);
        assertThat(orderSnapshot.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        assertThat(confirmationRequest.getOrderSnapshot().getId()).isEqualTo(orderSnapshot.getId());
        assertThat(confirmationRequest.getToken()).isNotBlank();
        assertThat(confirmationRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(confirmationRequest.getDeliveryDate()).hasToString("2026-06-12");
        assertThat(confirmationRequest.getDeliveryWindowStart()).hasToString("10:00");
        assertThat(confirmationRequest.getDeliveryWindowEnd()).hasToString("11:00");
        assertThat(confirmationRequest.isActive()).isTrue();
        assertThat(confirmationRequest.getSentAt()).isNotNull();
        assertThat(confirmationRequest.getExpiresAt()).isAfter(confirmationRequest.getSentAt());

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(OrderSnapshot.class), any(ConfirmationRequest.class));
    }

    @Test
    void createConfirmationRequest_smsChannel_createsOrderSnapshotAndConfirmationRequest() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(smsRequest("A-SMS-1024")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value("A-SMS-1024"))
                .andExpect(jsonPath("$.confirmationStatus").value("SENT"));

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);

        OrderSnapshot orderSnapshot = orderSnapshots.get(0);
        ConfirmationRequest confirmationRequest = confirmationRequests.get(0);

        assertThat(orderSnapshot.getExternalOrderId()).isEqualTo("A-SMS-1024");
        assertThat(orderSnapshot.getCustomerName()).isEqualTo("Max Muller");
        assertThat(orderSnapshot.getCustomerEmail()).isNull();
        assertThat(orderSnapshot.getCustomerPhoneNumber()).isEqualTo("+491701234567");
        assertThat(orderSnapshot.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        assertThat(confirmationRequest.getOrderSnapshot().getId()).isEqualTo(orderSnapshot.getId());
        assertThat(confirmationRequest.getToken()).isNotBlank();
        assertThat(confirmationRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(confirmationRequest.isActive()).isTrue();

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(OrderSnapshot.class), any(ConfirmationRequest.class));
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

        List<OrderSnapshot> orderSnapshots = orderSnapshotRepository.findAll();
        List<ConfirmationRequest> confirmationRequests = confirmationRequestRepository.findAll();

        assertThat(orderSnapshots).hasSize(1);
        assertThat(confirmationRequests).hasSize(1);
        assertThat(confirmationRequests.get(0).isActive()).isTrue();

        Mockito.verify(notificationService, times(1))
                .sendConfirmationRequest(any(OrderSnapshot.class), any(ConfirmationRequest.class));
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
        assertThat(activeRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequest(any(OrderSnapshot.class), any(ConfirmationRequest.class));
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

        OrderSnapshot orderSnapshot = orderSnapshots.get(0);

        assertThat(orderSnapshot.getCustomerEmail()).isNull();
        assertThat(orderSnapshot.getCustomerPhoneNumber()).isEqualTo("+491701234567");

        ConfirmationRequest activeRequest = confirmationRequests.stream()
                .filter(ConfirmationRequest::isActive)
                .findFirst()
                .orElseThrow();

        assertThat(activeRequest.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);

        Mockito.verify(notificationService, times(2))
                .sendConfirmationRequest(any(OrderSnapshot.class), any(ConfirmationRequest.class));
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
                "2026-06-12",
                deliveryWindowStart,
                deliveryWindowEnd,
                24
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
                "2026-06-12",
                "10:00",
                "11:00",
                24
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
                "2026-06-12",
                "10:00",
                "11:00",
                24
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
                "2026-06-12",
                "10:00",
                "11:00",
                24
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
                "2026-06-12",
                "10:00",
                "11:00",
                24
        ));
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
            Integer responseDeadlineHours
    ) {
    }
}
