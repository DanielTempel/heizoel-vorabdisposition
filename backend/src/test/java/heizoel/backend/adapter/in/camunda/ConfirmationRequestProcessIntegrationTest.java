package heizoel.backend.adapter.in.camunda;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.application.service.confirmation.CreateConfirmationRequestService;
import heizoel.backend.application.service.confirmation.SubmitCustomerResponseService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.NotificationDeliveryStatus;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "camunda.bpm.auto-deployment-enabled=true",
        "camunda.bpm.deployment-resource-pattern[0]=classpath*:processes/*.bpmn",
        "camunda.bpm.job-execution.enabled=false"
})
@Import(ConfirmationRequestProcessIntegrationTest.MutableClockConfiguration.class)
class ConfirmationRequestProcessIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-07T08:00:00Z");
    private static final String PARENT_PROCESS_KEY = "confirmation-request-process";

    private static final String SEND_ACTIVITY = "ServiceTask_SendConfirmationRequest";
    private static final String RETRY_TIMER_ACTIVITY = "Timer_WaitBeforeDeliveryRetry";
    private static final String DEADLINE_TIMER_ACTIVITY = "Timer_ResponseDeadlineReached";
    private static final String MARK_FAILED_ACTIVITY = "ServiceTask_MarkDeliveryFailed";
    private static final String CALLBACK_ACTIVITY = "ServiceTask_SendDispoCallback";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    ManagementService managementService;

    @Autowired
    ConfirmationWorkflowService confirmationWorkflowService;

    @Autowired
    CreateConfirmationRequestService createConfirmationRequestService;

    @Autowired
    SubmitCustomerResponseService submitCustomerResponseService;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    CustomerResponseRepository customerResponseRepository;

    @Autowired
    MutableClock clock;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    DispoStatusCallbackService dispoStatusCallbackService;

    @BeforeEach
    void setUp() {
        reset(notificationService, dispoStatusCallbackService);
        setBothClocks(INITIAL_TIME);
        deleteRunningProcesses();
        customerResponseRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @AfterEach
    void resetEngineClock() {
        ClockUtil.reset();
    }

    @Test
    void startCreatesAsyncSendJob() {
        ProcessFixture fixture = createPendingFixture();

        ProcessInstance parent = startProcess(fixture.request());

        Job sendJob = job(parent.getId(), SEND_ACTIVITY);
        assertThat(sendJob).isNotNull();
        assertThat(sendJob.getRetries()).isPositive();
        assertThat(runtimeService.getVariable(parent.getId(), "confirmationRequestId"))
                .isEqualTo(fixture.request().getId());
        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt")).isEqualTo(0);
        assertThat(runtimeService.getVariable(parent.getId(), "maxDeliveryAttempts")).isEqualTo(3);
        verifyNoInteractions(notificationService);
    }

    @Test
    void successfulSendMarksSentAndCreatesTwoMessagesAndDeadlineTimer() {
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startProcess(fixture.request());

        execute(job(parent.getId(), SEND_ACTIVITY));

        ConfirmationRequest request = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(request.isActive()).isTrue();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);

        List<EventSubscription> messages = runtimeService
                .createEventSubscriptionQuery()
                .processInstanceId(parent.getId())
                .eventType("message")
                .list();
        assertThat(messages)
                .extracting(EventSubscription::getEventName)
                .containsExactlyInAnyOrder(
                        "CustomerResponseReceived",
                        "ConfirmationRequestSuperseded"
                );

        Job deadline = job(parent.getId(), DEADLINE_TIMER_ACTIVITY);
        assertThat(deadline.getDuedate().toInstant()).isEqualTo(request.getExpiresAt());
        assertThat(messages).hasSize(2);
        assertThat(deadline).isNotNull();
    }

    @Test
    void customerResponseCreatesCallbackJobWithCorrectVariables() {
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startAndSendSuccessfully(fixture);

        confirmationWorkflowService.notifyCustomerResponseReceived(
                fixture.request().getId(),
                fixture.order().getId(),
                ConfirmationStatus.CONFIRMED,
                "Please call first"
        );

        assertThat(runtimeService.getVariable(parent.getId(), "orderId"))
                .isEqualTo(fixture.order().getId());
        assertThat(runtimeService.getVariable(parent.getId(), "confirmationStatus"))
                .isEqualTo(ConfirmationStatus.CONFIRMED.name());
        assertThat(runtimeService.getVariable(parent.getId(), "customerComment"))
                .isEqualTo("Please call first");
        assertThat(job(parent.getId(), CALLBACK_ACTIVITY)).isNotNull();
    }

    @Test
    void successfulCallbackEndsConfirmationProcess() {
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startAndSendSuccessfully(fixture);
        confirmationWorkflowService.notifyCustomerResponseReceived(
                fixture.request().getId(),
                fixture.order().getId(),
                ConfirmationStatus.CONFIRMED,
                "Please call first"
        );

        execute(job(parent.getId(), CALLBACK_ACTIVITY));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(parent.getId())
                .singleResult()).isNull();
    }

    @Test
    void supersededMessageEndsParentWithoutCreatingCallback() {
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startAndSendSuccessfully(fixture);

        confirmationWorkflowService.notifyConfirmationRequestSuperseded(fixture.request().getId());

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(parent.getId())
                .singleResult()).isNull();
        assertThat(managementService.createJobQuery()
                .processInstanceId(parent.getId())
                .activityId(CALLBACK_ACTIVITY)
                .count()).isZero();
        verifyNoInteractions(dispoStatusCallbackService);
    }

    @Test
    void responseDeadlineMarksNoResponseAndCreatesCallbackJob() {
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startAndSendSuccessfully(fixture);
        ConfirmationRequest sent = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        Job deadline = job(parent.getId(), DEADLINE_TIMER_ACTIVITY);

        setBothClocks(sent.getExpiresAt().plusMillis(1));
        execute(deadline);

        ConfirmationRequest timedOut = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertThat(timedOut.isActive()).isFalse();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.NO_RESPONSE);
        assertThat(runtimeService.getVariable(parent.getId(), "confirmationStatus"))
                .isEqualTo(ConfirmationStatus.NO_RESPONSE.name());
        assertThat(runtimeService.getVariable(parent.getId(), "customerComment")).isNull();

        execute(job(parent.getId(), CALLBACK_ACTIVITY));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(parent.getId())
                .singleResult()).isNull();
    }

    @Test
    void threeRetryableFailuresUseOneThenFiveMinuteDelaysAndEndFailed() {
        doThrow(retryableFailure())
                .when(notificationService)
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startProcess(fixture.request());

        execute(job(parent.getId(), SEND_ACTIVITY));
        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt")).isEqualTo(1);
        Job firstRetry = job(parent.getId(), RETRY_TIMER_ACTIVITY);
        assertThat(firstRetry.getDuedate().toInstant()).isEqualTo(INITIAL_TIME.plus(Duration.ofMinutes(1)));

        setBothClocks(firstRetry.getDuedate().toInstant());
        execute(firstRetry);
        execute(job(parent.getId(), SEND_ACTIVITY));
        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt")).isEqualTo(2);
        Job secondRetry = job(parent.getId(), RETRY_TIMER_ACTIVITY);
        assertThat(secondRetry.getDuedate().toInstant())
                .isEqualTo(INITIAL_TIME.plus(Duration.ofMinutes(6)));

        setBothClocks(secondRetry.getDuedate().toInstant());
        execute(secondRetry);
        execute(job(parent.getId(), SEND_ACTIVITY));
        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt")).isEqualTo(3);
        assertThat(managementService.createJobQuery()
                .processInstanceId(parent.getId())
                .activityId(RETRY_TIMER_ACTIVITY)
                .count()).isZero();

        execute(job(parent.getId(), MARK_FAILED_ACTIVITY));

        ConfirmationRequest failed = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        assertThat(failed.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(failed.isActive()).isFalse();
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(parent.getId())
                .count()).isZero();
        verifyNoInteractions(dispoStatusCallbackService);
    }

    @Test
    void permanentFailureGoesDirectlyToFailed() {
        doThrow(new EmailSettingsNotConfiguredException("Mail sender is not configured"))
                .when(notificationService)
                .sendConfirmationRequest(any(Order.class), any(ConfirmationRequest.class));
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startProcess(fixture.request());

        execute(job(parent.getId(), SEND_ACTIVITY));

        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt")).isEqualTo(3);
        assertThat(managementService.createJobQuery()
                .processInstanceId(parent.getId())
                .activityId(RETRY_TIMER_ACTIVITY)
                .count()).isZero();
        execute(job(parent.getId(), MARK_FAILED_ACTIVITY));
        assertThat(confirmationRequestRepository.findById(fixture.request().getId()).orElseThrow()
                .getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
    }

    @Test
    void callbackHttpFailureDecrementsRetriesAndCreatesIncidentWhenExhausted() {
        doThrow(new RuntimeException("DISPO unavailable"))
                .when(dispoStatusCallbackService)
                .sendStatusUpdate(any());
        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startAndSendSuccessfully(fixture);
        confirmationWorkflowService.notifyCustomerResponseReceived(
                fixture.request().getId(),
                fixture.order().getId(),
                ConfirmationStatus.CONFIRMED,
                "Please call first"
        );
        Object deliveryAttemptBeforeCallback = runtimeService.getVariable(
                parent.getId(),
                "deliveryAttempt"
        );
        Job callbackJob = job(parent.getId(), CALLBACK_ACTIVITY);
        int initialRetries = callbackJob.getRetries();

        while (callbackJob.getRetries() > 0) {
            String callbackJobId = callbackJob.getId();
            assertThatThrownBy(() -> managementService.executeJob(callbackJobId))
                    .isInstanceOf(RuntimeException.class);
            callbackJob = managementService.createJobQuery().jobId(callbackJobId).singleResult();
        }

        assertThat(initialRetries).isEqualTo(5);
        assertThat(callbackJob.getRetries()).isZero();
        List<Incident> incidents = runtimeService.createIncidentQuery()
                .processInstanceId(parent.getId())
                .list();
        assertThat(incidents).hasSize(1);
        assertThat(incidents.get(0).getIncidentType()).isEqualTo("failedJob");
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(parent.getId())
                .singleResult()).isNotNull();
        assertThat(runtimeService.getVariable(parent.getId(), "deliveryAttempt"))
                .isEqualTo(deliveryAttemptBeforeCallback);
    }

    @Test
    void customerConfirmAndDispoSupersedePreserveSerializedFinalState() throws Exception {
        ProcessFixture fixture = createPendingFixture();
        startAndSendSuccessfully(fixture);
        CountDownLatch confirmationApplied = new CountDownLatch(1);
        CountDownLatch releaseConfirmation = new CountDownLatch(1);
        CountDownLatch supersedeStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            Future<Void> confirmation = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    submitCustomerResponseService.submitCustomerResponse(
                            new SubmitCustomerResponseCommand(
                                    fixture.request().getToken(),
                                    CustomerResponseType.CONFIRM,
                                    "Confirmed concurrently"
                            )
                    );
                    confirmationApplied.countDown();
                    awaitLatch(releaseConfirmation);
                });
                return null;
            });
            assertThat(confirmationApplied.await(10, TimeUnit.SECONDS)).isTrue();

            Future<?> supersede = executor.submit(() -> {
                supersedeStarted.countDown();
                return createConfirmationRequestService.createConfirmationRequest(
                        changedCommand(fixture, CommunicationChannel.SMS)
                );
            });
            assertThat(supersedeStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> supersede.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseConfirmation.countDown();
            confirmation.get(10, TimeUnit.SECONDS);
            supersede.get(10, TimeUnit.SECONDS);
        } finally {
            releaseConfirmation.countDown();
            executor.shutdownNow();
        }

        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        ConfirmationRequest oldRequest = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        List<CustomerResponse> responses = customerResponseRepository.findAll();
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getResponseType()).isEqualTo(CustomerResponseType.CONFIRM);
        assertThat(oldRequest.isActive()).isFalse();
    }

    @Test
    void unexpectedSendFailureUsesTechnicalRetryWithoutAdvancingBusinessAttempt() {
        doThrow(new RuntimeException("Unexpected failure"))
                .when(notificationService)
                .sendConfirmationRequest(
                        any(Order.class),
                        any(ConfirmationRequest.class)
                );

        ProcessFixture fixture = createPendingFixture();
        ProcessInstance parent = startProcess(fixture.request());

        Job sendJob = job(parent.getId(), SEND_ACTIVITY);

        assertThat(sendJob.getRetries()).isEqualTo(3);

        assertThatThrownBy(() ->
                managementService.executeJob(sendJob.getId())
        ).isInstanceOf(RuntimeException.class);

        Job failedJob = managementService
                .createJobQuery()
                .jobId(sendJob.getId())
                .singleResult();

        assertThat(failedJob.getRetries()).isEqualTo(2);

        assertThat(
                runtimeService.getVariable(
                        parent.getId(),
                        "deliveryAttempt"
                )
        ).isEqualTo(0);

        assertThat(
                managementService.createJobQuery()
                        .processInstanceId(parent.getId())
                        .activityId(RETRY_TIMER_ACTIVITY)
                        .count()
        ).isZero();

        ConfirmationRequest request =
                confirmationRequestRepository
                        .findById(fixture.request().getId())
                        .orElseThrow();

        assertThat(request.isPending()).isTrue();
    }

    @Test
    void concurrentConfirmAndRejectPersistExactlyOneResponse() throws Exception {
        ProcessFixture fixture = createPendingFixture();
        startAndSendSuccessfully(fixture);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> confirm = concurrentResponse(
                    executor,
                    ready,
                    start,
                    fixture.request().getToken(),
                    CustomerResponseType.CONFIRM
            );
            Future<Throwable> reject = concurrentResponse(
                    executor,
                    ready,
                    start,
                    fixture.request().getToken(),
                    CustomerResponseType.REJECT
            );
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Throwable confirmFailure = confirm.get(10, TimeUnit.SECONDS);
            Throwable rejectFailure = reject.get(10, TimeUnit.SECONDS);
            assertThat(Arrays.asList(confirmFailure, rejectFailure))
                    .filteredOn(Objects::isNull)
                    .hasSize(1);
            assertThat(Arrays.asList(confirmFailure, rejectFailure))
                    .filteredOn(Objects::nonNull)
                    .allSatisfy(failure -> assertThat(failure)
                            .isInstanceOfAny(
                                    ConfirmationRequestInactiveException.class,
                                    CustomerResponseAlreadyExistsException.class
                            ))
                    .hasSize(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        ConfirmationRequest request = confirmationRequestRepository
                .findById(fixture.request().getId())
                .orElseThrow();
        List<CustomerResponse> responses = customerResponseRepository.findAll();
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(responses).hasSize(1);
        assertThat(request.isActive()).isFalse();
        if (responses.get(0).getResponseType() == CustomerResponseType.CONFIRM) {
            assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        } else {
            assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.REJECTED);
        }
    }

    private ProcessInstance startAndSendSuccessfully(ProcessFixture fixture) {
        ProcessInstance parent = startProcess(fixture.request());
        execute(job(parent.getId(), SEND_ACTIVITY));
        return parent;
    }

    private Future<Throwable> concurrentResponse(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            String token,
            CustomerResponseType responseType
    ) {
        return executor.submit(() -> {
            ready.countDown();
            awaitLatch(start);
            try {
                submitCustomerResponseService.submitCustomerResponse(
                        new SubmitCustomerResponseCommand(token, responseType, responseType.name())
                );
                return null;
            } catch (RuntimeException exception) {
                return exception;
            }
        });
    }

    private CreateConfirmationRequestCommand changedCommand(
            ProcessFixture fixture,
            CommunicationChannel channel
    ) {
        Order order = fixture.order();
        return new CreateConfirmationRequestCommand(
                new CompanyContext(order.getCompany().getId()),
                order.getExternalOrderId(),
                order.getTour().getTourNumber(),
                order.getTour().getVehicleLicensePlate(),
                order.getCustomerName(),
                channel,
                order.getCustomerEmail(),
                order.getCustomerPhoneNumber(),
                order.getDeliveryAddress(),
                order.getProduct(),
                order.getQuantityLiters(),
                LocalDate.of(2026, 8, 10),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                24,
                order.getPriceDisplayText()
        );
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while coordinating concurrent test");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted", exception);
        }
    }

    private ProcessInstance startProcess(ConfirmationRequest request) {
        confirmationWorkflowService.startDeliveryProcess(request.getId());
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PARENT_PROCESS_KEY)
                .processInstanceBusinessKey(request.getId().toString())
                .singleResult();
    }

    private Job job(String processInstanceId, String activityId) {
        return managementService.createJobQuery()
                .processInstanceId(processInstanceId)
                .activityId(activityId)
                .singleResult();
    }

    private void execute(Job job) {
        assertThat(job).as("job must exist before execution").isNotNull();
        managementService.executeJob(job.getId());
    }

    private ProcessFixture createPendingFixture() {
        String unique = UUID.randomUUID().toString();
        Company company = companyRepository.save(Company.create(
                "Company-" + unique,
                "api-key-" + unique,
                "http://localhost/dispo-callback"
        ));
        Order order = orderRepository.save(Order.create(
                company,
                "ORDER-" + unique,
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        ));
        ConfirmationRequest request = confirmationRequestRepository.save(
                ConfirmationRequest.createPending(
                        order,
                        "token-" + unique,
                        CommunicationChannel.EMAIL,
                        DeliverySlot.of(
                                LocalDate.of(2026, 8, 10),
                                LocalTime.of(10, 0),
                                LocalTime.of(12, 0)
                        ),
                        24
                )
        );
        return new ProcessFixture(order, request);
    }

    private NotificationDeliveryException retryableFailure() {
        return new NotificationDeliveryException(
                CommunicationChannel.EMAIL,
                "SMTP temporarily unavailable",
                new RuntimeException("connection refused")
        );
    }

    private void setBothClocks(Instant instant) {
        clock.set(instant);
        ClockUtil.setCurrentTime(Date.from(instant));
    }

    private void deleteRunningProcesses() {
        List<String> processInstanceIds = runtimeService.createProcessInstanceQuery()
                .list()
                .stream()
                .map(ProcessInstance::getId)
                .toList();
        for (String processInstanceId : processInstanceIds) {
            if (runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult() != null) {
                runtimeService.deleteProcessInstance(processInstanceId, "test cleanup");
            }
        }
    }

    private record ProcessFixture(Order order, ConfirmationRequest request) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(INITIAL_TIME, ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        private MutableClock(Instant initialInstant, ZoneId zone) {
            this.instant = new AtomicReference<>(initialInstant);
            this.zone = zone;
        }

        void set(Instant newInstant) {
            instant.set(newInstant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant.get(), newZone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
