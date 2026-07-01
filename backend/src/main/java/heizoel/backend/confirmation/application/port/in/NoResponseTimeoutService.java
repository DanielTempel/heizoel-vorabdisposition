package heizoel.backend.confirmation.application.port.in;

public interface NoResponseTimeoutService {

    void handleTimeout(Long confirmationRequestId);
}

