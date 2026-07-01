package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;

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

