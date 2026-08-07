package heizoel.backend.application.port.out.workflow;

public interface ConfirmationDeliveryWorkflowService {

    void startDeliveryProcess(Long confirmationRequestId);
}

