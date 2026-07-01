package heizoel.backend.confirmation.domain.exception;

public class ConfirmationRequestExpiredException extends RuntimeException {
    public ConfirmationRequestExpiredException(String message) {
        super(message);
    }
}

