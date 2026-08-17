package heizoel.backend.application.exception;

public class TestEmailDeliveryException extends RuntimeException {

    public TestEmailDeliveryException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}