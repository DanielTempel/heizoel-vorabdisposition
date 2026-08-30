package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestResult;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.service.confirmation.SendConfirmationRequestService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendConfirmationRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T08:00:00Z");

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    NotificationService notificationService;

    SendConfirmationRequestService service;

    @BeforeEach
    void setUp() {
        service = new SendConfirmationRequestService(
                confirmationRequestRepository,
                orderRepository,
                notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void successMarksRequestAndOrderAsSent() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, futureDeliverySlot());
        mockRequest(order, request);

        SendConfirmationRequestResult result = service.send(1L);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.SENT);
        assertThat(result.responseDeadlineAt()).isEqualTo(NOW.plusSeconds(24 * 60 * 60));
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(request.isActive()).isTrue();
        assertThat(request.getSentAt()).isEqualTo(NOW);
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        verify(notificationService).sendConfirmationRequest(order, request);
    }

    @Test
    void alreadySentReturnsPersistedDeadlineWithoutDuplicateNotification() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, futureDeliverySlot());
        request.markSent(NOW.minusSeconds(60));
        order.markSent();
        mockRequest(order, request);

        SendConfirmationRequestResult result = service.send(1L);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.SENT);
        assertThat(result.responseDeadlineAt()).isEqualTo(request.getExpiresAt());
        verify(notificationService, never()).sendConfirmationRequest(order, request);
    }

    @Test
    void notificationDeliveryExceptionIsRetryableAndKeepsRequestPending() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, futureDeliverySlot());
        mockRequest(order, request);
        doThrow(new NotificationDeliveryException(
                CommunicationChannel.EMAIL,
                "SMTP temporarily unavailable",
                new RuntimeException("connection refused")
        )).when(notificationService).sendConfirmationRequest(order, request);

        SendConfirmationRequestResult result = service.send(1L);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.RETRYABLE_FAILURE);
        assertThat(result.responseDeadlineAt()).isNull();
        assertThat(request.isPending()).isTrue();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
    }

    @Test
    void missingEmailSettingsIsPermanentAndKeepsRequestPending() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, futureDeliverySlot());
        mockRequest(order, request);
        doThrow(new EmailSettingsNotConfiguredException("Mail sender is not configured"))
                .when(notificationService).sendConfirmationRequest(order, request);

        SendConfirmationRequestResult result = service.send(1L);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.PERMANENT_FAILURE);
        assertThat(request.isPending()).isTrue();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
    }

    @Test
    void pastDeliveryWindowIsPermanentAndSkipsNotification() {
        Order order = order();
        DeliverySlot pastSlot = DeliverySlot.of(
                LocalDate.of(2026, Month.AUGUST, 7),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );
        ConfirmationRequest request = pendingRequest(order, pastSlot);
        mockRequest(order, request);

        SendConfirmationRequestResult result = service.send(1L);

        assertThat(result.outcome()).isEqualTo(SendConfirmationRequestResult.Outcome.PERMANENT_FAILURE);
        assertThat(request.isPending()).isTrue();
        verify(notificationService, never()).sendConfirmationRequest(order, request);
    }

    @Test
    void unknownRequestIsRejectedBeforeNotification() {
        when(orderRepository.findByConfirmationRequestIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(1L))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(confirmationRequestRepository, notificationService);
    }

    @Test
    void failedRequestIsRejectedWithoutNotification() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order, futureDeliverySlot());
        request.markDeliveryFailed();
        mockRequest(order, request);

        assertThatThrownBy(() -> service.send(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending confirmation request can be sent.");

        verifyNoInteractions(notificationService);
    }

    private void mockRequest(Order order, ConfirmationRequest request) {
        when(orderRepository.findByConfirmationRequestIdForUpdate(1L))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));
    }

    private ConfirmationRequest pendingRequest(Order order, DeliverySlot deliverySlot) {
        return ConfirmationRequest.createPending(
                order,
                "token",
                CommunicationChannel.EMAIL,
                deliverySlot,
                24
        );
    }

    private DeliverySlot futureDeliverySlot() {
        return DeliverySlot.of(
                LocalDate.of(2026, Month.AUGUST, 10),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
    }

    private Order order() {
        return Order.create(
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
        );
    }
}
