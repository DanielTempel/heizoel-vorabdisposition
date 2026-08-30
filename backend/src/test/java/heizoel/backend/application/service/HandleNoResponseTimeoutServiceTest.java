package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.service.workflow.HandleNoResponseTimeoutService;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleNoResponseTimeoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T08:00:00Z");

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    HandleNoResponseTimeoutService service;

    @BeforeEach
    void setUp() {
        service = new HandleNoResponseTimeoutService(
                confirmationRequestRepository,
                orderRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void activeExpiredRequestBecomesNoResponse() {
        Order order = spy(order());
        when(order.getId()).thenReturn(22L);
        RequestFixture fixture = sentRequest(order, NOW.minusSeconds(25 * 60 * 60));
        mockRequest(fixture);

        Long orderId = service.handleTimeout(11L);

        assertThat(orderId).isEqualTo(22L);
        assertThat(fixture.request().isActive()).isFalse();
        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.NO_RESPONSE);
    }

    @Test
    void inactiveRequestIsRejected() {
        RequestFixture fixture = sentRequest(order(), NOW.minusSeconds(25 * 60 * 60));
        fixture.request().markInactive();
        mockRequest(fixture);

        assertThatThrownBy(() -> service.handleTimeout(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only an active confirmation request can time out.");

        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
    }

    @Test
    void requestBeforeDeadlineIsRejected() {
        RequestFixture fixture = sentRequest(order(), NOW.minusSeconds(60 * 60));
        mockRequest(fixture);

        assertThatThrownBy(() -> service.handleTimeout(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Confirmation request has not expired yet.");

        assertThat(fixture.request().isActive()).isTrue();
        assertThat(fixture.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
    }

    @Test
    void unknownRequestIsRejectedWithoutRepositoryFollowUp() {
        when(orderRepository.findByConfirmationRequestIdForUpdate(11L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleTimeout(11L))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(confirmationRequestRepository);
    }

    private void mockRequest(RequestFixture fixture) {
        when(orderRepository.findByConfirmationRequestIdForUpdate(11L))
                .thenReturn(Optional.of(fixture.order()));
        when(confirmationRequestRepository.findById(11L))
                .thenReturn(Optional.of(fixture.request()));
    }

    private RequestFixture sentRequest(Order order, Instant sentAt) {
        ConfirmationRequest request = ConfirmationRequest.createPending(
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
        request.markSent(sentAt);
        order.markSent();
        return new RequestFixture(order, request);
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

    private record RequestFixture(Order order, ConfirmationRequest request) {
    }
}
