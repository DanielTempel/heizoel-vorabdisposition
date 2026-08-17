package heizoel.backend.application.port.in.workflow;

public interface MarkDeliveryFailedUseCase {

    void markDeliveryFailed(Long confirmationRequestId);
}