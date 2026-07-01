package heizoel.backend.confirmation.adapter.out.persistence;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationRequestRepository extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findTopByOrderSnapshotOrderByIdDesc(OrderSnapshot orderSnapshot);
    Optional<ConfirmationRequest> findByToken(String token);

}

