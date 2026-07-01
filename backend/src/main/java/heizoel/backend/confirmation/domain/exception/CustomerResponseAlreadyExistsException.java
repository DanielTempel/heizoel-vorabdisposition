package heizoel.backend.confirmation.domain.exception;

public class CustomerResponseAlreadyExistsException extends RuntimeException {
    public CustomerResponseAlreadyExistsException(String message) {
        super(message);
    }
}

