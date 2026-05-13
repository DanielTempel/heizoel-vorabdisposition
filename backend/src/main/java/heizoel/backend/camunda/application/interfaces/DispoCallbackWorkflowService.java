package heizoel.backend.camunda.application.interfaces;

import heizoel.backend.dispo.domain.ConfirmationStatus;

public interface DispoCallbackWorkflowService {

    void startDispoCallbackProcess(
            String externalOrderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );
}
