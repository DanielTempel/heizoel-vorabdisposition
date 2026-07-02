package heizoel.backend.confirmation.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;

import java.util.Optional;

public interface ConfirmationRequestRepositoryPort {

    Optional<ConfirmationRequest> findLatestByOrderSnapshot(OrderSnapshot orderSnapshot);

    Optional<ConfirmationRequest> findByToken(String token);

    Optional<ConfirmationRequest> findById(Long id);

    ConfirmationRequest save(ConfirmationRequest confirmationRequest);
}
