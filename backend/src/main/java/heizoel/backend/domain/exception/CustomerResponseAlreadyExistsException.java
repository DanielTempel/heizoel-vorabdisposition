package heizoel.backend.domain.exception;

public class CustomerResponseAlreadyExistsException extends RuntimeException {
    public CustomerResponseAlreadyExistsException(String message) {
        super(message);
    }
}

