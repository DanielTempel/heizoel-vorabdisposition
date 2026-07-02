package heizoel.backend.confirmation.adapter.out.persistence;

import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
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
