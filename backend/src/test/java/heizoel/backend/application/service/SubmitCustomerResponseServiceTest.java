package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.application.service.confirmation.SubmitCustomerResponseService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitCustomerResponseServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T08:00:00Z");
    private static final String TOKEN = "response-token";

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    CustomerResponseRepository customerResponseRepository;

    @Mock
    ConfirmationWorkflowService confirmationWorkflowService;

    @Mock
    NotificationService notificationService;

    SubmitCustomerResponseService service;

    @BeforeEach
    void setUp() {
        service = serviceAt();
    }

    @Test
    void confirmPersistsResponseAndNotifiesWorkflow() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        stubIds(fixture);
        mockRequest(fixture);
        when(customerResponseRepository.existsByConfirmationRequest(fixture.request()))
                .thenReturn(false);
        SubmitCustomerResponseCommand command = new SubmitCustomerResponseCommand(
                TOKEN,
                CustomerResponseType.CONFIRM,
                "Please call first"
        );

        service.submitCustomerResponse(command);

        CustomerResponse response = captureSavedResponse();
        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(fixture.request().isActive()).isFalse();
        assertThat(response.getConfirmationRequest()).isSameAs(fixture.request());
        assertThat(response.getResponseType()).isEqualTo(CustomerResponseType.CONFIRM);
        assertThat(response.getComment()).isEqualTo("Please call first");
        assertThat(response.getReceivedAt()).isEqualTo(NOW);
        verify(confirmationWorkflowService).notifyCustomerResponseReceived(
                11L,
                22L,
                ConfirmationStatus.CONFIRMED,
                "Please call first"
        );
        verify(notificationService).sendCustomerResponseReceived(
                fixture.order(),
                fixture.request(),
                CustomerResponseType.CONFIRM
        );
    }

    @Test
    void rejectPersistsResponseAndNotifiesWorkflow() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        stubIds(fixture);
        mockRequest(fixture);
        when(customerResponseRepository.existsByConfirmationRequest(fixture.request()))
                .thenReturn(false);

        service.submitCustomerResponse(new SubmitCustomerResponseCommand(
                TOKEN,
                CustomerResponseType.REJECT,
                "Not today"
        ));

        CustomerResponse response = captureSavedResponse();
        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.REJECTED);
        assertThat(fixture.request().isActive()).isFalse();
        assertThat(response.getResponseType()).isEqualTo(CustomerResponseType.REJECT);
        verify(confirmationWorkflowService).notifyCustomerResponseReceived(
                11L,
                22L,
                ConfirmationStatus.REJECTED,
                "Not today"
        );
    }

    @Test
    void inactiveTokenIsRejectedBeforeResponseLookup() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        fixture.request().markInactive();
        mockRequest(fixture);

        assertThatThrownBy(() -> service.submitCustomerResponse(command()))
                .isInstanceOf(ConfirmationRequestInactiveException.class)
                .hasMessage("This confirmation request is no longer active.");

        verifyNoInteractions(customerResponseRepository, confirmationWorkflowService, notificationService);
    }

    @Test
    void expiredTokenIsRejected() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(25 * 60 * 60));
        mockRequest(fixture);

        assertThatThrownBy(() -> service.submitCustomerResponse(command()))
                .isInstanceOf(ConfirmationRequestExpiredException.class)
                .hasMessage("This confirmation request has expired.");

        verifyNoInteractions(customerResponseRepository, confirmationWorkflowService, notificationService);
    }

    @Test
    void olderTokenIsRejectedAsNotFoundWhenNewerRequestExists() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        when(orderRepository.findByConfirmationRequestTokenForUpdate(TOKEN))
                .thenReturn(Optional.of(fixture.order()));
        when(confirmationRequestRepository.findLatestByToken(TOKEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitCustomerResponse(command()))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(customerResponseRepository, confirmationWorkflowService, notificationService);
    }

    @Test
    void duplicateResponseIsRejectedWithoutChangingState() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        mockRequest(fixture);
        when(customerResponseRepository.existsByConfirmationRequest(fixture.request()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submitCustomerResponse(command()))
                .isInstanceOf(CustomerResponseAlreadyExistsException.class)
                .hasMessage("A customer response already exists for this confirmation request.");

        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        assertThat(fixture.request().isActive()).isTrue();
        verify(customerResponseRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(confirmationWorkflowService, notificationService);
    }

    @Test
    void followUpNotificationFailureDoesNotUndoAcceptedResponse() {
        RequestFixture fixture = activeRequestSentAt(NOW.minusSeconds(60 * 60));
        stubIds(fixture);
        mockRequest(fixture);
        when(customerResponseRepository.existsByConfirmationRequest(fixture.request()))
                .thenReturn(false);
        doThrow(new NotificationDeliveryException(
                CommunicationChannel.EMAIL,
                "Follow-up delivery failed",
                new RuntimeException("SMTP unavailable")
        )).when(notificationService).sendCustomerResponseReceived(
                fixture.order(),
                fixture.request(),
                CustomerResponseType.CONFIRM
        );

        service.submitCustomerResponse(command());

        CustomerResponse response = captureSavedResponse();
        assertThat(response.getResponseType()).isEqualTo(CustomerResponseType.CONFIRM);
        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(fixture.request().isActive()).isFalse();
        verify(confirmationWorkflowService).notifyCustomerResponseReceived(
                11L,
                22L,
                ConfirmationStatus.CONFIRMED,
                "Please call first"
        );
    }

    private SubmitCustomerResponseService serviceAt() {
        return new SubmitCustomerResponseService(
                confirmationRequestRepository,
                orderRepository,
                customerResponseRepository,
                confirmationWorkflowService,
                notificationService,
                Clock.fixed(SubmitCustomerResponseServiceTest.NOW, ZoneOffset.UTC)
        );
    }

    private void mockRequest(RequestFixture fixture) {
        when(orderRepository.findByConfirmationRequestTokenForUpdate(TOKEN))
                .thenReturn(Optional.of(fixture.order()));
        when(confirmationRequestRepository.findLatestByToken(TOKEN))
                .thenReturn(Optional.of(fixture.request()));
    }

    private CustomerResponse captureSavedResponse() {
        ArgumentCaptor<CustomerResponse> captor = ArgumentCaptor.forClass(CustomerResponse.class);
        verify(customerResponseRepository).save(captor.capture());
        return captor.getValue();
    }

    private SubmitCustomerResponseCommand command() {
        return new SubmitCustomerResponseCommand(
                TOKEN,
                CustomerResponseType.CONFIRM,
                "Please call first"
        );
    }

    private RequestFixture activeRequestSentAt(Instant sentAt) {
        Order order = spy(Order.create(
                Company.create("Company", "api-key-hash", "http://localhost/callback"),
                "ORDER-1",
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                "customer@example.com",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        ));
        ConfirmationRequest request = spy(ConfirmationRequest.createPending(
                order,
                TOKEN,
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2026, Month.AUGUST, 10),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                ),
                24
        ));
        request.markSent(sentAt);
        order.markSent();
        return new RequestFixture(order, request);
    }

    private void stubIds(RequestFixture fixture) {
        when(fixture.order().getId()).thenReturn(22L);
        when(fixture.request().getId()).thenReturn(11L);
    }

    private record RequestFixture(Order order, ConfirmationRequest request) {
    }
}
