package heizoel.backend.application.port.out.workflow;

import heizoel.backend.domain.ConfirmationStatus;

public interface DispoCallbackWorkflowService {

    void startDispoCallbackProcess(
            Long orderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );
}

