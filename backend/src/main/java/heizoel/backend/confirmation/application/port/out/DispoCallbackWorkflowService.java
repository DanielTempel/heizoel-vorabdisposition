package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.ConfirmationStatus;

public interface DispoCallbackWorkflowService {

    void startDispoCallbackProcess(
            String externalOrderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );
}

