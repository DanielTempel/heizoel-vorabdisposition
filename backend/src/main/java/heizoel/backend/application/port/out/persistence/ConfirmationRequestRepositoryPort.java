package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;

import java.util.Optional;

public interface ConfirmationRequestRepositoryPort {

    Optional<ConfirmationRequest> findLatestByOrderSnapshot(OrderSnapshot orderSnapshot);

    Optional<ConfirmationRequest> findByToken(String token);

    Optional<ConfirmationRequest> findById(Long id);

    ConfirmationRequest save(ConfirmationRequest confirmationRequest);
}
