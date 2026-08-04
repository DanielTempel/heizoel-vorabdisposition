package heizoel.backend.application.service;

import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.application.service.workflow.HandleNoResponseTimeoutService;
import heizoel.backend.domain.*;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleNoResponseTimeoutServiceTest {

    @Spy
    Clock clock = Clock.system(ZoneOffset.UTC);

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    CustomerResponseRepository customerResponseRepository;

    @Mock
    DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @InjectMocks
    HandleNoResponseTimeoutService service;

    @Test
    void handleTimeout_doesNothingWhenRequestIsInactive() {
        ConfirmationRequest confirmationRequest = confirmationRequest(false, Instant.now(clock).minusSeconds(1));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));

        service.handleTimeout(1L);

        verifyNoInteractions(customerResponseRepository, orderRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_doesNothingWhenCustomerResponseAlreadyExists() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now(clock).minusSeconds(1));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(true);

        service.handleTimeout(1L);

        verify(customerResponseRepository).existsByConfirmationRequest(confirmationRequest);
        verifyNoInteractions(orderRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_doesNothingWhenDeadlineIsStillInTheFuture() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now(clock).plusSeconds(60));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(customerResponseRepository).existsByConfirmationRequest(confirmationRequest);
        verifyNoInteractions(orderRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_marksNoResponseWhenRequestIsActiveUnansweredAndExpired() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now(clock).minusSeconds(1));
        Order order = confirmationRequest.getOrder();

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(confirmationRequestRepository).save(confirmationRequest);
        verify(orderRepository).save(order);
        verify(dispoCallbackWorkflowService).startDispoCallbackProcess(
                order.getId(),
                ConfirmationStatus.NO_RESPONSE,
                null
        );
    }

    @Test
    void handleTimeout_throwsNotFoundWhenRequestDoesNotExist() {
        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleTimeout(1L))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(customerResponseRepository, orderRepository, dispoCallbackWorkflowService);
    }

    private ConfirmationRequest confirmationRequest(boolean active, Instant expiresAt) {
        Order order = Order.create(
                Company.create(
                        "Company", "api-key-hash", "http://localhost/callback"
                ),
                "A-TIMEOUT-1", Tour.of("17", "WÜ-AB 123"),
                "Customer", "customer@example.com", null,
                "Address", "Heating oil", 1000, "1,000 EUR"
        );
        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        java.time.LocalDate.now(clock).plusDays(1),
                        java.time.LocalTime.of(10, 0),
                        java.time.LocalTime.of(11, 0)
                ),
                expiresAt.minus(Duration.ofHours(24)),
                24
        );
        if (!active) {
            confirmationRequest.markInactive();
        }

        return confirmationRequest;
    }
}

