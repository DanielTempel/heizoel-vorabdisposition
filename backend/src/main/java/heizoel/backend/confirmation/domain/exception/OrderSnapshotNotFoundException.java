package heizoel.backend.confirmation.domain.exception;

public class OrderSnapshotNotFoundException extends RuntimeException {
    public OrderSnapshotNotFoundException(String message) {
        super(message);
    }
}
