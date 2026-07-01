package heizoel.backend.confirmation.application.port.in;

public interface HandleNoResponseTimeoutUseCase {

    void handleTimeout(Long confirmationRequestId);
}
