package heizoel.backend.dispo.domain.repository;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationRequestRepository extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findTopByOrderSnapshotOrderByIdDesc(OrderSnapshot orderSnapshot);
    Optional<ConfirmationRequest> findByToken(String token);

}
