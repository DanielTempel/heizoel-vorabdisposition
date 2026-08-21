package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestResult;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestUseCase;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.NotificationDeliveryStatus;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@Sql(
        scripts = "/db/test/configure-test-company.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class SendConfirmationRequestConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("heizoel_concurrent_send_test")
                    .withUsername("heizoel")
                    .withPassword("heizoel");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    SendConfirmationRequestUseCase sendConfirmationRequestUseCase;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void concurrentRetriesSendNotificationOnlyOnce() throws Exception {
        Long requestId = createPendingRequest();
        CountDownLatch callersReady = new CountDownLatch(2);
        CountDownLatch startCalls = new CountDownLatch(1);
        CountDownLatch notificationEntered = new CountDownLatch(1);
        CountDownLatch releaseNotification = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        doAnswer(invocation -> {
            notificationEntered.countDown();
            if (!releaseNotification.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out while coordinating concurrent sends."
                );
            }
            return null;
        }).when(notificationService)
                .sendConfirmationRequest(any(), any());

        Future<SendConfirmationRequestResult> first = null;
        Future<SendConfirmationRequestResult> second = null;
        try {
            first = executor.submit(() -> sendAfterStart(
                    requestId,
                    callersReady,
                    startCalls
            ));
            second = executor.submit(() -> sendAfterStart(
                    requestId,
                    callersReady,
                    startCalls
            ));

            assertThat(callersReady.await(5, TimeUnit.SECONDS)).isTrue();
            startCalls.countDown();
            assertThat(notificationEntered.await(10, TimeUnit.SECONDS))
                    .isTrue();
            releaseNotification.countDown();

            SendConfirmationRequestResult firstResult =
                    first.get(10, TimeUnit.SECONDS);
            SendConfirmationRequestResult secondResult =
                    second.get(10, TimeUnit.SECONDS);

            assertThat(List.of(firstResult, secondResult))
                    .allSatisfy(result -> assertThat(result.outcome())
                            .isEqualTo(
                                    SendConfirmationRequestResult.Outcome.SENT
                            ));
            assertThat(firstResult.responseDeadlineAt())
                    .isCloseTo(
                            secondResult.responseDeadlineAt(),
                            within(1, ChronoUnit.MICROS)
                    );
        } finally {
            startCalls.countDown();
            releaseNotification.countDown();
            executor.shutdownNow();
        }

        verify(notificationService, times(1))
                .sendConfirmationRequest(any(), any());

        ConfirmationRequest persistedRequest = confirmationRequestRepository
                .findById(requestId)
                .orElseThrow();
        Order persistedOrder = orderRepository
                .findById(persistedRequest.getOrder().getId())
                .orElseThrow();
        assertThat(persistedRequest.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(persistedRequest.isActive()).isTrue();
        assertThat(persistedOrder.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);
    }

    private SendConfirmationRequestResult sendAfterStart(
            Long requestId,
            CountDownLatch callersReady,
            CountDownLatch startCalls
    ) throws Exception {
        callersReady.countDown();
        if (!startCalls.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Timed out before starting concurrent sends."
            );
        }
        return sendConfirmationRequestUseCase.send(requestId);
    }

    private Long createPendingRequest() {
        Company company = companyRepository.findAll()
                .stream()
                .filter(candidate -> candidate.getName()
                        .equals("Minova Heizöl GmbH"))
                .findFirst()
                .orElseThrow();
        Order order = orderRepository.save(Order.create(
                company,
                "CONCURRENT-SEND",
                Tour.of("CONCURRENT-TOUR", "WUE-CS 1"),
                "Concurrent Customer",
                "concurrent@example.test",
                null,
                "Concurrent Street 1, 97070 Würzburg",
                "Heating oil",
                2_500,
                "2,500 EUR"
        ));
        ConfirmationRequest request = confirmationRequestRepository.save(
                ConfirmationRequest.createPending(
                        order,
                        "concurrent-send-token",
                        CommunicationChannel.EMAIL,
                        DeliverySlot.of(
                                LocalDate.of(2099, 6, 12),
                                LocalTime.of(10, 0),
                                LocalTime.of(11, 0)
                        ),
                        24
                )
        );
        return request.getId();
    }
}
