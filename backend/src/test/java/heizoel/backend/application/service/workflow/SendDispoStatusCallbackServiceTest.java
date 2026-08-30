package heizoel.backend.application.service.workflow;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.port.in.workflow.SendDispoStatusCallbackCommand;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendDispoStatusCallbackServiceTest {

    @Mock
    DispoStatusCallbackService dispoStatusCallbackService;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    ConfirmationRequest latestRequest;

    SendDispoStatusCallbackService service;

    @BeforeEach
    void setUp() {
        service = new SendDispoStatusCallbackService(
                dispoStatusCallbackService,
                orderRepository,
                confirmationRequestRepository
        );
    }

    @Test
    void sendsCompanyCallbackForLatestRequest() {
        Order order = order();
        when(orderRepository.findById(71L)).thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.of(latestRequest));
        when(latestRequest.getId()).thenReturn(101L);

        service.sendDispoStatusCallback(new SendDispoStatusCallbackCommand(
                101L,
                71L,
                ConfirmationStatus.REJECTED,
                "Please call me"
        ));

        verify(dispoStatusCallbackService).sendStatusUpdate(
                new DispoStatusCallbackRequest(
                        "http://localhost:8081/api/confirmation-status-updates",
                        "ORDER-CALLBACK",
                        ConfirmationStatus.REJECTED,
                        "Please call me"
                )
        );
    }

    @Test
    void staleRequestDoesNotSendCallback() {
        Order order = order();
        when(orderRepository.findById(71L)).thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.of(latestRequest));
        when(latestRequest.getId()).thenReturn(102L);

        service.sendDispoStatusCallback(new SendDispoStatusCallbackCommand(
                101L,
                71L,
                ConfirmationStatus.CONFIRMED,
                null
        ));

        verifyNoInteractions(dispoStatusCallbackService);
    }

    @Test
    void missingLatestRequestDoesNotSendCallback() {
        Order order = order();
        when(orderRepository.findById(71L)).thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendDispoStatusCallback(
                new SendDispoStatusCallbackCommand(
                        101L,
                        71L,
                        ConfirmationStatus.CONFIRMED,
                        null
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No confirmation request found for order.");

        verifyNoInteractions(dispoStatusCallbackService);
    }

    @Test
    void missingOrderDoesNotSendCallback() {
        when(orderRepository.findById(71L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendDispoStatusCallback(
                new SendDispoStatusCallbackCommand(
                        101L,
                        71L,
                        ConfirmationStatus.CONFIRMED,
                        null
                )
        ))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order was not found.");

        verifyNoInteractions(confirmationRequestRepository, dispoStatusCallbackService);
    }

    private Order order() {
        return Order.create(
                Company.create(
                        "Company",
                        "api-key-hash",
                        "http://localhost:8081/api/confirmation-status-updates"
                ),
                "ORDER-CALLBACK",
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
