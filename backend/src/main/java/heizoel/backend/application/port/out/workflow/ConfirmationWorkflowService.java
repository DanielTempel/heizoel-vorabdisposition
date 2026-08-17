package heizoel.backend.application.port.out.workflow;

import heizoel.backend.domain.ConfirmationStatus;

public interface ConfirmationWorkflowService {

    void startDeliveryProcess(Long confirmationRequestId);

    void notifyCustomerResponseReceived(
            Long confirmationRequestId,
            Long orderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    );

    void notifyConfirmationRequestSuperseded(
            Long confirmationRequestId
    );
}

