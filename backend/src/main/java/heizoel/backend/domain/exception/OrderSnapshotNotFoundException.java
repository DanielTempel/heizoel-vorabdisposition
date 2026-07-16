package heizoel.backend.domain.exception;

public class OrderSnapshotNotFoundException extends RuntimeException {
    public OrderSnapshotNotFoundException(String message) {
        super(message);
    }
}
