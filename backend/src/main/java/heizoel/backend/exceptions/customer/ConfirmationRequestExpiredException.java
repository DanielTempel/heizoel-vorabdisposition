package heizoel.backend.exceptions.customer;

public class ConfirmationRequestExpiredException extends RuntimeException {
    public ConfirmationRequestExpiredException(String message) {
        super(message);
    }
}
