package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;

public interface ConfirmationWorkflowService {

    void startTimeoutProcess(ConfirmationRequest confirmationRequest);
}

