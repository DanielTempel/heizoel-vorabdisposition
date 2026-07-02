package heizoel.backend.confirmation.adapter.out.persistence;

import heizoel.backend.confirmation.application.port.out.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSnapshotRepository
        extends JpaRepository<OrderSnapshot, Long>, OrderSnapshotRepositoryPort {

    @Override
    Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId);

}

