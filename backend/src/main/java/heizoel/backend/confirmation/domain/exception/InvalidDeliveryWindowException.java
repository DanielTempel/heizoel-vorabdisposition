package heizoel.backend.confirmation.domain.exception;

public class InvalidDeliveryWindowException extends RuntimeException {
    public InvalidDeliveryWindowException(String message) {
        super(message);
    }
}

