package heizoel.backend.dispo.application.interfaces;

import heizoel.backend.dispo.application.model.ConfirmationRequestData;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;

import java.util.Optional;

public interface ConfirmationRequestService {

    Optional<ConfirmationRequest> findLatestRequest(OrderSnapshot orderSnapshot);

    ConfirmationRequest create(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData data
    );

    void markInactive(ConfirmationRequest confirmationRequest);

    boolean hasSameData(
            ConfirmationRequest confirmationRequest,
            ConfirmationRequestData data
    );

    Optional<ConfirmationRequest> findByToken(String token);
    Optional<ConfirmationRequest> findById(Long id);
}
