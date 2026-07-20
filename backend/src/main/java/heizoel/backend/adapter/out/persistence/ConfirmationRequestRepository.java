package heizoel.backend.adapter.out.persistence;

import heizoel.backend.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationRequestRepository
        extends JpaRepository<ConfirmationRequest, Long>, ConfirmationRequestRepositoryPort {

    Optional<ConfirmationRequest> findTopByOrderSnapshotOrderByIdDesc(OrderSnapshot orderSnapshot);

    @Override
    default Optional<ConfirmationRequest> findLatestByOrderSnapshot(OrderSnapshot orderSnapshot) {
        return findTopByOrderSnapshotOrderByIdDesc(orderSnapshot);
    }

    @Override
    Optional<ConfirmationRequest> findByToken(String token);

}
