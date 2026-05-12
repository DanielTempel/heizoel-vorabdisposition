package heizoel.backend.dispo.domain.repository;

import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSnapshotRepository extends JpaRepository<OrderSnapshot, Long> {

    Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId);

}
