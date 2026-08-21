package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.service.confirmation.MarkDeliveryFailedService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkDeliveryFailedServiceTest {

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    MarkDeliveryFailedService service;

    @BeforeEach
    void setUp() {
        service = new MarkDeliveryFailedService(orderRepository, confirmationRequestRepository);
    }

    @Test
    void pendingRequestBecomesFailed() {
        Order order = order();
        order.markRejected();
        ConfirmationRequest request = pendingRequest(order);
        mockRequest(order, request);

        service.markDeliveryFailed(11L);

        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(request.isActive()).isFalse();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
    }

    @Test
    void alreadyFailedRequestIsNoOp() {
        Order order = order();
        order.markRejected();
        ConfirmationRequest request = pendingRequest(order);
        request.markDeliveryFailed();
        mockRequest(order, request);

        service.markDeliveryFailed(11L);

        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.REJECTED);
    }

    @Test
    void sentRequestIsRejected() {
        Order order = order();
        ConfirmationRequest request = pendingRequest(order);
        request.markSent(Instant.parse("2026-08-07T08:00:00Z"));
        order.markSent();
        mockRequest(order, request);

        assertThatThrownBy(() -> service.markDeliveryFailed(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending confirmation request can be marked as failed.");

        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
    }

    @Test
    void unknownRequestIsRejectedWithoutRepositoryFollowUp() {
        when(orderRepository.findByConfirmationRequestIdForUpdate(11L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markDeliveryFailed(11L))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(confirmationRequestRepository);
    }

    private void mockRequest(Order order, ConfirmationRequest request) {
        when(orderRepository.findByConfirmationRequestIdForUpdate(11L))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findById(11L))
                .thenReturn(Optional.of(request));
    }

    private ConfirmationRequest pendingRequest(Order order) {
        return ConfirmationRequest.createPending(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2026, Month.AUGUST, 10),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                ),
                24
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
