package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateConfirmationRequestServiceTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2099, Month.JUNE, 12);
    private static final LocalTime DELIVERY_START = LocalTime.of(10, 0);
    private static final Instant SENT_AT = Instant.parse("2099-06-10T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(SENT_AT, ZoneOffset.UTC);

    @Mock
    CompanyRepository companyRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    ConfirmationRequestStarter confirmationRequestStarter;

    @Mock
    ConfirmationWorkflowService confirmationWorkflowService;

    @Mock
    Company company;

    CreateConfirmationRequestService service;

    @BeforeEach
    void setUp() {
        service = new CreateConfirmationRequestService(
                companyRepository,
                orderRepository,
                confirmationRequestRepository,
                confirmationWorkflowService,
                confirmationRequestStarter,
                CLOCK
        );
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
    }

    @Test
    void newOrderDelegatesPendingRequestCreationToStarter() {
        CreateConfirmationRequestCommand command = command();
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        verify(confirmationRequestStarter).createAndStart(
                orderCaptor.capture(),
                eq(CommunicationChannel.EMAIL),
                eq(deliverySlot(command)),
                eq(24)
        );
        assertThat(orderCaptor.getValue().getExternalOrderId()).isEqualTo("ORDER-1");
        assertThat(orderCaptor.getValue().getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.OPEN);
        verifyNoInteractions(confirmationWorkflowService);
    }

    @Test
    void identicalPendingRequestIsReused() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, command);
        mockExisting(order, request);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(request.isPending()).isTrue();
        assertNoNewRequestOrProcess();
    }

    @Test
    void changedPendingRequestIsUpdatedWithoutStartingSecondProcess() {
        CreateConfirmationRequestCommand original = command();
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, original);
        mockExisting(order, request);
        CreateConfirmationRequestCommand changed = command(
                "Changed Customer",
                CommunicationChannel.SMS,
                DELIVERY_START.plusHours(1),
                48
        );

        CreateConfirmationRequestResult result = service.createConfirmationRequest(changed);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(order.getCustomerName()).isEqualTo("Changed Customer");
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(request.getDeliverySlot()).isEqualTo(deliverySlot(changed));
        assertThat(request.getResponseDeadlineHours()).isEqualTo(48);
        assertThat(request.isPending()).isTrue();
        assertNoNewRequestOrProcess();
    }

    @Test
    void changedActiveSentRequestIsSupersededAndReplaced() {
        CreateConfirmationRequestCommand original = command();
        Order order = order();
        ConfirmationRequest oldRequest = spy(sentRequest(order, original));
        when(oldRequest.getId()).thenReturn(7L);
        mockExisting(order, oldRequest);
        CreateConfirmationRequestCommand changed = command(
                "Customer",
                CommunicationChannel.SMS,
                DELIVERY_START,
                24
        );

        CreateConfirmationRequestResult result = service.createConfirmationRequest(changed);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(oldRequest.isActive()).isFalse();
        verify(confirmationWorkflowService).notifyConfirmationRequestSuperseded(7L);
        verify(confirmationRequestStarter).createAndStart(
                order,
                CommunicationChannel.SMS,
                deliverySlot(changed),
                24
        );
    }

    @Test
    void failedRequestIsReplacedByNewPendingRequest() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest failedRequest = pendingRequest(order, command);
        failedRequest.markDeliveryFailed();
        mockExisting(order, failedRequest);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(failedRequest.isDeliveryFailed()).isTrue();
        verify(confirmationRequestStarter).createAndStart(
                order,
                CommunicationChannel.EMAIL,
                deliverySlot(command),
                24
        );
        verifyNoInteractions(confirmationWorkflowService);
    }


    @Test
    void identicalActiveSentRequestIsReused() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = sentRequest(order, command);
        mockExisting(order, request);

        CreateConfirmationRequestResult result =
                service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus())
                .isEqualTo(ConfirmationStatus.SENT);

        assertThat(request.isActive()).isTrue();

        assertNoNewRequestOrProcess();
    }


    @Test
    void noResponseRequestIsReplacedByNewPendingRequest() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest oldRequest = sentRequest(order, command);
        oldRequest.markInactive();
        order.markNoResponse();
        mockExisting(order, oldRequest);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(oldRequest.isActive()).isFalse();
        verify(confirmationRequestStarter).createAndStart(
                order,
                CommunicationChannel.EMAIL,
                deliverySlot(command),
                24
        );
        verify(confirmationWorkflowService, never()).notifyConfirmationRequestSuperseded(any());
    }

    @Test
    void identicalConfirmedRequestIsReused() {
        assertIdenticalCompletedRequestIsReused(ConfirmationStatus.CONFIRMED);
    }

    @Test
    void identicalRejectedRequestIsReused() {
        assertIdenticalCompletedRequestIsReused(ConfirmationStatus.REJECTED);
    }

    private void assertIdenticalCompletedRequestIsReused(ConfirmationStatus status) {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = sentRequest(order, command);
        request.markInactive();
        if (status == ConfirmationStatus.CONFIRMED) {
            order.markConfirmed();
        } else {
            order.markRejected();
        }
        mockExisting(order, request);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(status);
        assertThat(request.isActive()).isFalse();
        assertNoNewRequestOrProcess();
    }

    private void assertNoNewRequestOrProcess() {
        verifyNoInteractions(
                confirmationRequestStarter,
                confirmationWorkflowService
        );
    }

    private void mockExisting(Order order, ConfirmationRequest request) {
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.of(request));
    }

    private Order order() {
        return Order.create(
                company,
                "ORDER-1",
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        );
    }

    private ConfirmationRequest pendingRequest(
            Order order,
            CreateConfirmationRequestCommand command
    ) {
        return ConfirmationRequest.createPending(
                order,
                "existing-token",
                command.communicationChannel(),
                deliverySlot(command),
                command.responseDeadlineHours()
        );
    }

    private ConfirmationRequest sentRequest(
            Order order,
            CreateConfirmationRequestCommand command
    ) {
        ConfirmationRequest request = pendingRequest(order, command);
        request.markSent(SENT_AT);
        order.markSent();
        return request;
    }

    private DeliverySlot deliverySlot(CreateConfirmationRequestCommand command) {
        return DeliverySlot.of(
                command.deliveryDate(),
                command.deliveryWindowStart(),
                command.deliveryWindowEnd()
        );
    }

    private CreateConfirmationRequestCommand command() {
        return command("Customer", CommunicationChannel.EMAIL, DELIVERY_START, 24);
    }

    private CreateConfirmationRequestCommand command(
            String customerName,
            CommunicationChannel channel,
            LocalTime deliveryStart,
            int responseDeadlineHours
    ) {
        return new CreateConfirmationRequestCommand(
                new CompanyContext(1L),
                "ORDER-1",
                "17",
                "WUE-AB 123",
                customerName,
                channel,
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                DELIVERY_DATE,
                deliveryStart,
                deliveryStart.plusHours(1),
                responseDeadlineHours,
                "1,000 EUR"
        );
    }
}
