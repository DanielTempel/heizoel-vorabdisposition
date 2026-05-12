package heizoel.backend.exceptions.customer;

public class CustomerResponseAlreadyExistsException extends RuntimeException {
    public CustomerResponseAlreadyExistsException(String message) {
        super(message);
    }
}
