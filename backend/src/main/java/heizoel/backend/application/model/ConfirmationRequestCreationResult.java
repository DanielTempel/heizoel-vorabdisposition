package heizoel.backend.application.model;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;

public record ConfirmationRequestCreationResult(
        OrderSnapshot orderSnapshot,
        ConfirmationRequest confirmationRequest,
        boolean created
) {
}
