package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSnapshotRepository extends JpaRepository<OrderSnapshot, Long> {

    Optional<OrderSnapshot> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );
}
