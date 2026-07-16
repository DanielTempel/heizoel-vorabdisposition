package heizoel.backend.confirmation.application.model;

import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.OrderSnapshot;

public record ConfirmationRequestCreationResult(
        OrderSnapshot orderSnapshot,
        ConfirmationRequest confirmationRequest,
        boolean created
) {
}
