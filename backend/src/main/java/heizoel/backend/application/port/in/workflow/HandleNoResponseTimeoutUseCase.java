package heizoel.backend.application.port.in.workflow;

public interface HandleNoResponseTimeoutUseCase {

    void handleTimeout(Long confirmationRequestId);
}
