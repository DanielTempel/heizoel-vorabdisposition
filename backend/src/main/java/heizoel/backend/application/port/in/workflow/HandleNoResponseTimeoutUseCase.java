package heizoel.backend.application.port.in.workflow;

public interface HandleNoResponseTimeoutUseCase {

    Long handleTimeout(Long confirmationRequestId);
}
