package heizoel.backend.application.service;

import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.application.service.workflow.HandleNoResponseTimeoutService;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleNoResponseTimeoutServiceTest {

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderSnapshotRepository orderSnapshotRepository;

    @Mock
    CustomerResponseRepository customerResponseRepository;

    @Mock
    DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @InjectMocks
    HandleNoResponseTimeoutService service;

    @Test
    void handleTimeout_doesNothingWhenRequestIsInactive() {
        ConfirmationRequest confirmationRequest = confirmationRequest(false, Instant.now().minusSeconds(1));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));

        service.handleTimeout(1L);

        verifyNoInteractions(customerResponseRepository, orderSnapshotRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_doesNothingWhenCustomerResponseAlreadyExists() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().minusSeconds(1));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(true);

        service.handleTimeout(1L);

        verify(customerResponseRepository).existsByConfirmationRequest(confirmationRequest);
        verifyNoInteractions(orderSnapshotRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_doesNothingWhenDeadlineIsStillInTheFuture() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().plusSeconds(60));

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(customerResponseRepository).existsByConfirmationRequest(confirmationRequest);
        verifyNoInteractions(orderSnapshotRepository, dispoCallbackWorkflowService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void handleTimeout_marksNoResponseWhenRequestIsActiveUnansweredAndExpired() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().minusSeconds(1));
        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        when(confirmationRequestRepository.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseRepository.existsByConfirmationRequest(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(confirmationRequestRepository).save(confirmationRequest);
        verify(orderSnapshotRepository).save(orderSnapshot);
        verify(dispoCallbackWorkflowService).startDispoCallbackProcess(
                orderSnapshot.getId(),
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

        verifyNoInteractions(customerResponseRepository, orderSnapshotRepository, dispoCallbackWorkflowService);
    }

    private ConfirmationRequest confirmationRequest(boolean active, Instant expiresAt) {
        OrderSnapshot orderSnapshot = OrderSnapshot.create(
                Company.create(
                        "Company", "api-key-hash", "http://localhost/callback"
                ),
                "A-TIMEOUT-1", "Customer", "customer@example.com", null,
                "Address", "Heating oil", 1000, "1,000 EUR"
        );
        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                orderSnapshot,
                "token",
                CommunicationChannel.EMAIL,
                java.time.LocalDate.now().plusDays(1),
                java.time.LocalTime.of(10, 0),
                java.time.LocalTime.of(11, 0),
                Instant.now(),
                expiresAt,
                24
        );
        if (!active) {
            confirmationRequest.markInactive();
        }

        return confirmationRequest;
    }
}

