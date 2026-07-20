package heizoel.backend.application.port.out.workflow;

import java.time.Instant;

public interface NoResponseWorkflowService {

    void startTimeoutProcess(Long confirmationRequestId, Instant responseDeadlineAt);
}

