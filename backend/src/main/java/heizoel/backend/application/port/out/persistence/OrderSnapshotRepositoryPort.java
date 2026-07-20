package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.OrderSnapshot;

import java.util.Optional;

public interface OrderSnapshotRepositoryPort {

    Optional<OrderSnapshot> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );

    Optional<OrderSnapshot> findById(Long id);

    OrderSnapshot save(OrderSnapshot orderSnapshot);
}
