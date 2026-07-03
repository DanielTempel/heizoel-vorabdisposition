package heizoel.backend.confirmation.adapter.persistence;

import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSnapshotRepository
        extends JpaRepository<OrderSnapshot, Long>, OrderSnapshotRepositoryPort {

    @Override
    Optional<OrderSnapshot> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );
}
