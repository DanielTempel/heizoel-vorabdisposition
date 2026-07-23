package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;

public record ConfirmationRequestCreationResult(
        Order order,
        ConfirmationRequest confirmationRequest,
        boolean created
) {
}
