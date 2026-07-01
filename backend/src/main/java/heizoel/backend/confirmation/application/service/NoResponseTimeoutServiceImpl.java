package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.DispoCallbackWorkflowService;
import heizoel.backend.confirmation.application.port.in.NoResponseTimeoutService;
import heizoel.backend.confirmation.application.port.out.CustomerResponseService;
import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NoResponseTimeoutServiceImpl implements NoResponseTimeoutService {

    private final ConfirmationRequestService confirmationRequestService;
    private final OrderSnapshotService orderSnapshotService;
    private final CustomerResponseService customerResponseService;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @Override
    @Transactional
    public void handleTimeout(Long confirmationRequestId) {
        ConfirmationRequest confirmationRequest = confirmationRequestService.findById(confirmationRequestId)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException("Confirmation request was not found."));

        if (!confirmationRequest.isActive()) {
            return;
        }

        if (customerResponseService.existsFor(confirmationRequest)) {
            return;
        }

        if (confirmationRequest.getExpiresAt().isAfter(Instant.now())) {
            return;
        }

        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        confirmationRequestService.markInactive(confirmationRequest);
        orderSnapshotService.updateStatus(orderSnapshot, ConfirmationStatus.NO_RESPONSE);

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                orderSnapshot.getExternalOrderId(),
                ConfirmationStatus.NO_RESPONSE,
                null
        );
    }

}

