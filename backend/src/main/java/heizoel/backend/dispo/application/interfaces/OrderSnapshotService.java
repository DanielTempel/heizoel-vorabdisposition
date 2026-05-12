package heizoel.backend.dispo.application.interfaces;

import heizoel.backend.dispo.application.model.OrderSnapshotData;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.ConfirmationStatus;

import java.util.Optional;

public interface OrderSnapshotService {

    Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId);

    OrderSnapshot create(OrderSnapshotData data);
    OrderSnapshot update(OrderSnapshot orderSnapshot, OrderSnapshotData data);
    boolean hasSameData(OrderSnapshot orderSnapshot, OrderSnapshotData data);

    OrderSnapshot updateStatus(OrderSnapshot orderSnapshot, ConfirmationStatus status);
}