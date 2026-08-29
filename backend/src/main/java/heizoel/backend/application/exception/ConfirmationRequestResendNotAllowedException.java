package heizoel.backend.application.exception;

public class ConfirmationRequestResendNotAllowedException extends RuntimeException {
    public ConfirmationRequestResendNotAllowedException(String message) {
        super(message);
    }
}
