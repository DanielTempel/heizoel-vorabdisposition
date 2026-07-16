package heizoel.backend.domain.exception;

public class ConfirmationRequestExpiredException extends RuntimeException {
    public ConfirmationRequestExpiredException(String message) {
        super(message);
    }
}

