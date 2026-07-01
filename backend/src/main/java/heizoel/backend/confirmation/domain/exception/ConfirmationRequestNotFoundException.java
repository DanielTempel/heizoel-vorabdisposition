package heizoel.backend.confirmation.domain.exception;

public class ConfirmationRequestNotFoundException extends RuntimeException {
    public ConfirmationRequestNotFoundException(String message) {
        super(message);
    }
}

