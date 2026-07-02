package heizoel.backend.confirmation.application.port.out.workflow;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public interface DispoCallbackWorkflowService {

    void startDispoCallbackProcess(
            String externalOrderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );
}

