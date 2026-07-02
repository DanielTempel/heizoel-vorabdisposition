package heizoel.backend.confirmation.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.OrderSnapshot;

import java.util.Optional;

public interface OrderSnapshotRepositoryPort {

    Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId);

    OrderSnapshot save(OrderSnapshot orderSnapshot);
}
