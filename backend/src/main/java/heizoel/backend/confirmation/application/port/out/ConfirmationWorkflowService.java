package heizoel.backend.confirmation.application.port.out;

import java.time.Instant;

public interface ConfirmationWorkflowService {

    void startTimeoutProcess(Long confirmationRequestId, Instant responseDeadlineAt);
}

