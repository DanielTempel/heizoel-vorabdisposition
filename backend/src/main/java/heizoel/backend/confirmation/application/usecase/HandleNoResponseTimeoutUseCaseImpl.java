package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.timeout.HandleNoResponseTimeoutUseCase;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.OrderSnapshot;
import heizoel.backend.domain.exception.ConfirmationRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HandleNoResponseTimeoutUseCaseImpl implements HandleNoResponseTimeoutUseCase {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final OrderSnapshotRepositoryPort orderSnapshotRepository;
    private final CustomerResponseRepositoryPort customerResponseRepository;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @Override
    @Transactional
    public void handleTimeout(Long confirmationRequestId) {

        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findById(confirmationRequestId)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException("Confirmation request was not found."));

        if (!confirmationRequest.isActive()) {
            return;
        }

        if (customerResponseRepository.existsByConfirmationRequest(confirmationRequest)) {
            return;
        }

        if (confirmationRequest.getExpiresAt().isAfter(Instant.now())) {
            return;
        }

        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);
        orderSnapshot.markNoResponse();
        orderSnapshotRepository.save(orderSnapshot);

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                orderSnapshot.getId(),
                ConfirmationStatus.NO_RESPONSE,
                null
        );
    }

}

