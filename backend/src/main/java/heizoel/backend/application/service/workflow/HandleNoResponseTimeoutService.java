package heizoel.backend.application.service.workflow;

import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HandleNoResponseTimeoutService implements HandleNoResponseTimeoutUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final OrderSnapshotRepository orderSnapshotRepository;
    private final CustomerResponseRepository customerResponseRepository;
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

