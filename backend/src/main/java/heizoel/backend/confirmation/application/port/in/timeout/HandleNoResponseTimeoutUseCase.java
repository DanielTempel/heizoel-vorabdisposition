package heizoel.backend.confirmation.application.port.in.timeout;

public interface HandleNoResponseTimeoutUseCase {

    void handleTimeout(Long confirmationRequestId);
}
