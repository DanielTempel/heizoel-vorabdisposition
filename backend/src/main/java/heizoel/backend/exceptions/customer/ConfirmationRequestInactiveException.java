package heizoel.backend.exceptions.customer;

public class ConfirmationRequestInactiveException extends RuntimeException {
    public ConfirmationRequestInactiveException(String message) {
        super(message);
    }
}
