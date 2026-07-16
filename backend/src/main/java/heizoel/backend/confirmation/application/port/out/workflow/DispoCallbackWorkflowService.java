package heizoel.backend.confirmation.application.port.out.workflow;

import heizoel.backend.domain.model.enumeration.ConfirmationStatus;

public interface DispoCallbackWorkflowService {

    void startDispoCallbackProcess(
            Long orderSnapshotId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );
}

