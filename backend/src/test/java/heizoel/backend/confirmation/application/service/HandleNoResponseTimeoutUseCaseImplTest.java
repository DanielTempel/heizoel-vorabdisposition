package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.usecase.HandleNoResponseTimeoutUseCaseImpl;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
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
class HandleNoResponseTimeoutUseCaseImplTest {

    @Mock
    ConfirmationRequestRepositoryPort confirmationRequestRepository;

    @Mock
    OrderSnapshotRepositoryPort orderSnapshotRepository;

    @Mock
    CustomerResponseRepositoryPort customerResponseRepository;

    @Mock
    DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @InjectMocks
    HandleNoResponseTimeoutUseCaseImpl service;

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
                "A-TIMEOUT-1",
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
                "A-TIMEOUT-1", "Customer", "customer@example.com", null,
                "Address", "Heating oil", 1000, "1,000 EUR"
        );
        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                orderSnapshot,
                "token",
                heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel.EMAIL,
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

