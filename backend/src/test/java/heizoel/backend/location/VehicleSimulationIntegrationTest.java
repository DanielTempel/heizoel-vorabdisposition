package heizoel.backend.location;

import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
import heizoel.backend.dispo.domain.repository.ConfirmationRequestRepository;
import heizoel.backend.dispo.domain.repository.OrderSnapshotRepository;
import heizoel.backend.location.domain.LocationTrackingSnapshot;
import heizoel.backend.location.domain.VehicleSimulationStatus;
import heizoel.backend.location.persistence.LocationTrackingSnapshotRepository;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
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

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class VehicleSimulationIntegrationTest {

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
        registry.add("camunda.bpm.job-execution.enabled", () -> "false");
        registry.add("heizoel.location.simulation.tick-interval-millis", () -> "200");
        registry.add("heizoel.location.simulation.minimum-step-kilometers", () -> "0.15");
        registry.add("heizoel.location.simulation.maximum-step-kilometers", () -> "0.40");
    }

    @MockitoBean
    JavaMailSender javaMailSender;

    @MockitoBean
    ConfirmationNotificationService confirmationNotificationService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderSnapshotRepository orderSnapshotRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    CustomerResponseRepository customerResponseRepository;

    @Autowired
    LocationTrackingSnapshotRepository locationTrackingSnapshotRepository;

    @BeforeEach
    void cleanDatabase() {
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderSnapshotRepository.deleteAll();
        locationTrackingSnapshotRepository.deleteAll();
        Mockito.reset(confirmationNotificationService);
    }

    @Test
    void shouldStartVehicleSimulationSuccessfully() throws Exception {
        String externalOrderId = uniqueOrderId("A-TRACKING-START");
        createDispoConfirmationRequest(externalOrderId);

        mockMvc.perform(post("/api/dispo/confirmation-requests/{externalOrderId}/vehicle-simulation/start", externalOrderId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId").value(externalOrderId))
                .andExpect(jsonPath("$.simulationStatus").value(VehicleSimulationStatus.STARTED.name()));

        mockMvc.perform(post("/api/dispo/confirmation-requests/{externalOrderId}/vehicle-simulation/start", externalOrderId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.simulationStatus").value(VehicleSimulationStatus.ALREADY_RUNNING.name()));
    }

    @Test
    void shouldUpdateStoredVehicleLocationWhileSimulationIsRunning() throws Exception {
        String externalOrderId = uniqueOrderId("A-TRACKING-MOVE");
        createDispoConfirmationRequest(externalOrderId);

        LocationTrackingSnapshot snapshotBeforeStart = locationTrackingSnapshotRepository
                .findByExternalOrderId(externalOrderId)
                .orElseThrow();
        double initialLocationX = snapshotBeforeStart.getLocationX();
        double initialLocationY = snapshotBeforeStart.getLocationY();
        double initialDistance = distanceInKilometers(
                snapshotBeforeStart.getLocationY(),
                snapshotBeforeStart.getLocationX(),
                snapshotBeforeStart.getTargetLocationY(),
                snapshotBeforeStart.getTargetLocationX()
        );

        mockMvc.perform(post("/api/dispo/confirmation-requests/{externalOrderId}/vehicle-simulation/start", externalOrderId))
                .andExpect(status().isAccepted());

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    LocationTrackingSnapshot updatedSnapshot = locationTrackingSnapshotRepository
                            .findByExternalOrderId(externalOrderId)
                            .orElseThrow();

                    assertThat(updatedSnapshot.getLocationX()).isNotEqualTo(initialLocationX);
                    assertThat(updatedSnapshot.getLocationY()).isNotEqualTo(initialLocationY);

                    double updatedDistance = distanceInKilometers(
                            updatedSnapshot.getLocationY(),
                            updatedSnapshot.getLocationX(),
                            updatedSnapshot.getTargetLocationY(),
                            updatedSnapshot.getTargetLocationX()
                    );

                    assertThat(updatedDistance).isLessThan(initialDistance);
                });
    }

    private void createDispoConfirmationRequest(String externalOrderId) throws Exception {
        String requestJson = """
                {
                  "externalOrderId": "%s",
                  "customerName": "Max Muller",
                  "communicationChannel": "EMAIL",
                  "customerEmail": "daniel@example.com",
                  "customerPhoneNumber": null,
                  "deliveryAddress": "Domstraße 40, 97070 Würzburg",
                  "locationX": 9.8820,
                  "locationY": 49.8166,
                  "targetLocationX": 9.9372,
                  "targetLocationY": 49.7935,
                  "product": "Heizöl Standard",
                  "quantityLiters": 3000,
                  "deliveryDate": "2026-06-12",
                  "deliveryWindowStart": "10:00",
                  "deliveryWindowEnd": "11:00",
                  "responseDeadlineHours": 24
                }
                """.formatted(externalOrderId);

        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    private String uniqueOrderId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private double distanceInKilometers(
            double startLatitude,
            double startLongitude,
            double targetLatitude,
            double targetLongitude
    ) {
        double earthRadiusKilometers = 6371.0D;
        double latitudeDistance = Math.toRadians(targetLatitude - startLatitude);
        double longitudeDistance = Math.toRadians(targetLongitude - startLongitude);
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(startLatitude))
                * Math.cos(Math.toRadians(targetLatitude))
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);

        return earthRadiusKilometers * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
