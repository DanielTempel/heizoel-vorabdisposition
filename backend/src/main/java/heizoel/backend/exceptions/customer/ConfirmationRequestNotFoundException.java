package heizoel.backend.exceptions.customer;

public class ConfirmationRequestNotFoundException extends RuntimeException {
    public ConfirmationRequestNotFoundException(String message) {
        super(message);
    }
}
