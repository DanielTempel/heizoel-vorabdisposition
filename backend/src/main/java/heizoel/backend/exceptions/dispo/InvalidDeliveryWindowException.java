package heizoel.backend.exceptions.dispo;

public class InvalidDeliveryWindowException extends RuntimeException {
    public InvalidDeliveryWindowException(String message) {
        super(message);
    }
}
