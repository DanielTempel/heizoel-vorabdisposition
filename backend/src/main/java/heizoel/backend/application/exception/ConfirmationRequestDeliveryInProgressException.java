package heizoel.backend.application.exception;

public class ConfirmationRequestDeliveryInProgressException extends RuntimeException {
    public ConfirmationRequestDeliveryInProgressException(String message) {
        super(message);
    }
}
