package heizoel.backend.camunda.application.interfaces;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;

public interface ConfirmationWorkflowService {

    void startTimeoutProcess(ConfirmationRequest confirmationRequest);
}
