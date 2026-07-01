package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;

import java.util.Optional;

public interface OrderSnapshotService {

    Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId);

    OrderSnapshot create(OrderSnapshotData data);
    OrderSnapshot update(OrderSnapshot orderSnapshot, OrderSnapshotData data);
    boolean hasSameData(OrderSnapshot orderSnapshot, OrderSnapshotData data);

    OrderSnapshot updateStatus(OrderSnapshot orderSnapshot, ConfirmationStatus status);
}
