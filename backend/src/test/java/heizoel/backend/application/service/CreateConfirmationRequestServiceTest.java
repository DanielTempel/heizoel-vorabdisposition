package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.out.token.TokenService;
import heizoel.backend.application.port.out.workflow.ConfirmationDeliveryWorkflowService;
import heizoel.backend.application.service.confirmation.CreateConfirmationRequestService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.NotificationDeliveryStatus;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateConfirmationRequestServiceTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2099, 6, 12);
    private static final LocalTime DELIVERY_WINDOW_START = LocalTime.of(10, 0);
    private static final Instant SENT_AT = Instant.parse("2099-06-10T10:00:00Z");

    @Mock
    CompanyRepository companyRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    TokenService tokenService;

    @Mock
    ConfirmationDeliveryWorkflowService confirmationDeliveryWorkflowService;

    @Mock
    Company company;

    @Mock
    ConfirmationRequest savedRequest;

    @InjectMocks
    CreateConfirmationRequestService service;

    @BeforeEach
    void setUpCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
    }

    @Test
    void createCreatesOpenOrderAndPendingRequestWhenOrderDoesNotExist() {
        CreateConfirmationRequestCommand command = command();
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubNewPendingRequest();

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order createdOrder = orderCaptor.getValue();
        ConfirmationRequest createdRequest = captureSavedRequest();

        assertThat(result.externalOrderId()).isEqualTo("ORDER-1");
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(createdOrder.getTour()).isEqualTo(Tour.of("17", "WUE-AB 123"));
        assertThat(createdRequest.getOrder()).isSameAs(createdOrder);
        assertThat(createdRequest.getToken()).isEqualTo("new-token");
        assertThat(createdRequest.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(createdRequest.isActive()).isFalse();
        assertThat(createdRequest.getSentAt()).isNull();
        assertThat(createdRequest.getExpiresAt()).isNull();
        verify(confirmationDeliveryWorkflowService).startDeliveryProcess(42L);
    }

    @Test
    void createCreatesPendingRequestWhenExistingOrderHasNoRequests() {
        CreateConfirmationRequestCommand command = command();
        Order order = order(Tour.of("OLD", "OLD-PLATE"), "Old Customer");
        order.markRejected();
        mockExistingOrder(order, Optional.empty());
        stubNewPendingRequest();

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        ConfirmationRequest createdRequest = captureSavedRequest();
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(order.getTour()).isEqualTo(Tour.of("17", "WUE-AB 123"));
        assertThat(order.getCustomerName()).isEqualTo("Customer");
        assertThat(createdRequest.getOrder()).isSameAs(order);
        assertThat(createdRequest.isPending()).isTrue();
        verify(orderRepository, never()).save(any());
        verify(confirmationDeliveryWorkflowService).startDeliveryProcess(42L);
    }

    @Test
    void createReusesIdenticalPendingRequestWithoutStartingSecondWorkflow() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, command);
        mockExistingOrder(order, Optional.of(request));

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(request.isPending()).isTrue();
        assertNoNewRequestWasStarted();
    }

    @Test
    void createUpdatesPendingRequestDataWithoutStartingSecondWorkflow() {
        CreateConfirmationRequestCommand originalCommand = command();
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, originalCommand);
        mockExistingOrder(order, Optional.of(request));
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.SMS,
                DELIVERY_WINDOW_START.plusHours(1),
                48
        );

        CreateConfirmationRequestResult result = service.createConfirmationRequest(changedCommand);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(request.getDeliverySlot()).isEqualTo(deliverySlot(changedCommand));
        assertThat(request.getResponseDeadlineHours()).isEqualTo(48);
        assertThat(request.isPending()).isTrue();
        assertNoNewRequestWasStarted();
    }

    @Test
    void createUpdatesOrderDataForPendingRequestWithoutStartingSecondWorkflow() {
        CreateConfirmationRequestCommand originalCommand = command();
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, originalCommand);
        mockExistingOrder(order, Optional.of(request));
        CreateConfirmationRequestCommand changedCommand = command(
                "18",
                "WUE-CD 456",
                "Changed Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                24
        );

        CreateConfirmationRequestResult result = service.createConfirmationRequest(changedCommand);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(order.getTour()).isEqualTo(Tour.of("18", "WUE-CD 456"));
        assertThat(order.getCustomerName()).isEqualTo("Changed Customer");
        assertThat(request.isPending()).isTrue();
        assertNoNewRequestWasStarted();
    }

    @Test
    void createReusesIdenticalActiveSentRequest() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = sentRequest(order, command);
        mockExistingOrder(order, Optional.of(request));

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        assertThat(request.isActive()).isTrue();
        assertNoNewRequestWasStarted();
    }

    @Test
    void createReusesIdenticalConfirmedRequest() {
        assertCompletedRequestIsReused(ConfirmationStatus.CONFIRMED);
    }

    @Test
    void createReusesIdenticalRejectedRequest() {
        assertCompletedRequestIsReused(ConfirmationStatus.REJECTED);
    }

    @Test
    void createCreatesNewPendingRequestAfterNoResponse() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest oldRequest = sentRequest(order, command);
        oldRequest.markInactive();
        order.markNoResponse();
        mockExistingOrder(order, Optional.of(oldRequest));
        stubNewPendingRequest();

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        ConfirmationRequest newRequest = captureSavedRequest();
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(newRequest).isNotSameAs(oldRequest);
        assertThat(newRequest.isPending()).isTrue();
        assertThat(oldRequest.isActive()).isFalse();
        verify(confirmationDeliveryWorkflowService).startDeliveryProcess(42L);
    }

    @Test
    void createCreatesNewPendingRequestAfterDeliveryFailure() {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest failedRequest = pendingRequest(order, command);
        failedRequest.markDeliveryFailed();
        mockExistingOrder(order, Optional.of(failedRequest));
        stubNewPendingRequest();

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        ConfirmationRequest newRequest = captureSavedRequest();
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(newRequest).isNotSameAs(failedRequest);
        assertThat(newRequest.isPending()).isTrue();
        assertThat(failedRequest.isDeliveryFailed()).isTrue();
        verify(confirmationDeliveryWorkflowService).startDeliveryProcess(42L);
    }

    @Test
    void createReplacesActiveSentRequestWhenDeliverySlotChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START.plusHours(1),
                24
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.newRequest().getDeliverySlot())
                .isEqualTo(deliverySlot(changedCommand));
    }

    @Test
    void createReplacesActiveSentRequestWhenCommunicationChannelChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.SMS,
                DELIVERY_WINDOW_START,
                24
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.newRequest().getCommunicationChannel())
                .isEqualTo(CommunicationChannel.SMS);
    }

    @Test
    void createReplacesActiveSentRequestWhenResponseDeadlineChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                48
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.newRequest().getResponseDeadlineHours()).isEqualTo(48);
    }

    @Test
    void createReplacesActiveSentRequestWhenCustomerNameChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-AB 123",
                "Changed Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                24
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.order().getCustomerName()).isEqualTo("Changed Customer");
    }

    @Test
    void createReplacesActiveSentRequestWhenTourNumberChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "18",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                24
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.order().getTour()).isEqualTo(Tour.of("18", "WUE-AB 123"));
    }

    @Test
    void createReplacesActiveSentRequestWhenVehicleLicensePlateChanges() {
        CreateConfirmationRequestCommand changedCommand = command(
                "17",
                "WUE-CD 456",
                "Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                24
        );

        RequestReplacement replacement = replaceActiveSentRequest(changedCommand);

        assertThat(replacement.order().getTour()).isEqualTo(Tour.of("17", "WUE-CD 456"));
    }

    private RequestReplacement replaceActiveSentRequest(
            CreateConfirmationRequestCommand changedCommand
    ) {
        CreateConfirmationRequestCommand originalCommand = command();
        Order order = order();
        ConfirmationRequest oldRequest = sentRequest(order, originalCommand);
        mockExistingOrder(order, Optional.of(oldRequest));
        stubNewPendingRequest();

        CreateConfirmationRequestResult result = service.createConfirmationRequest(changedCommand);

        ConfirmationRequest newRequest = captureSavedRequest();
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        assertThat(oldRequest.isActive()).isFalse();
        assertThat(newRequest.isPending()).isTrue();
        verify(confirmationDeliveryWorkflowService).startDeliveryProcess(42L);
        return new RequestReplacement(order, newRequest);
    }

    private record RequestReplacement(
            Order order,
            ConfirmationRequest newRequest
    ) {
    }

    private void assertCompletedRequestIsReused(ConfirmationStatus status) {
        CreateConfirmationRequestCommand command = command();
        Order order = order();
        ConfirmationRequest request = sentRequest(order, command);
        request.markInactive();
        if (status == ConfirmationStatus.CONFIRMED) {
            order.markConfirmed();
        } else {
            order.markRejected();
        }
        mockExistingOrder(order, Optional.of(request));

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.confirmationStatus()).isEqualTo(status);
        assertThat(request.isActive()).isFalse();
        assertNoNewRequestWasStarted();
    }

    private void stubNewPendingRequest() {
        when(tokenService.generateToken()).thenReturn("new-token");
        when(confirmationRequestRepository.save(any())).thenReturn(savedRequest);
        when(savedRequest.getId()).thenReturn(42L);
    }

    private ConfirmationRequest captureSavedRequest() {
        ArgumentCaptor<ConfirmationRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationRequestRepository).save(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    private void assertNoNewRequestWasStarted() {
        verify(orderRepository, never()).save(any());
        verify(confirmationRequestRepository, never()).save(any());
        verifyNoInteractions(tokenService, confirmationDeliveryWorkflowService);
    }

    private void mockExistingOrder(
            Order order,
            Optional<ConfirmationRequest> latestRequest
    ) {
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(latestRequest);
    }

    private Order order() {
        return order(Tour.of("17", "WUE-AB 123"), "Customer");
    }

    private Order order(Tour tour, String customerName) {
        return Order.create(
                company,
                "ORDER-1",
                tour,
                customerName,
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1000,
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
        return command(
                "17",
                "WUE-AB 123",
                "Customer",
                CommunicationChannel.EMAIL,
                DELIVERY_WINDOW_START,
                24
        );
    }

    private CreateConfirmationRequestCommand command(
            String tourNumber,
            String vehicleLicensePlate,
            String customerName,
            CommunicationChannel communicationChannel,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return new CreateConfirmationRequestCommand(
                new CompanyContext(1L),
                "ORDER-1",
                tourNumber,
                vehicleLicensePlate,
                customerName,
                communicationChannel,
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1000,
                DELIVERY_DATE,
                deliveryWindowStart,
                deliveryWindowStart.plusHours(1),
                responseDeadlineHours,
                "1,000 EUR"
        );
    }
}
