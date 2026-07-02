package heizoel.backend.confirmation.application.model;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;

public record ConfirmationRequestCreationResult(
        OrderSnapshot orderSnapshot,
        ConfirmationRequest confirmationRequest,
        boolean created
) {
}
