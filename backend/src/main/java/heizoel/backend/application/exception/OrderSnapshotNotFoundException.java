package heizoel.backend.application.exception;

public class OrderSnapshotNotFoundException extends RuntimeException {
    public OrderSnapshotNotFoundException(String message) {
        super(message);
    }
}
