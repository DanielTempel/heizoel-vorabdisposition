package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSnapshotRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );
}
