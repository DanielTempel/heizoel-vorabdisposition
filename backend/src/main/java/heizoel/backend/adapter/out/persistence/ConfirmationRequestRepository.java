package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationRequestRepository extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findTopByOrderSnapshotOrderByIdDesc(
            OrderSnapshot orderSnapshot
    );

    Optional<ConfirmationRequest> findByToken(String token);
}
