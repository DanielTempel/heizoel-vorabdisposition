package heizoel.backend.camunda.application.interfaces;

public interface NoResponseTimeoutService {

    void handleTimeout(Long confirmationRequestId);
}
