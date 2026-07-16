package heizoel.backend.confirmation.application.port.in.confirmation;

import heizoel.backend.domain.model.enumeration.ConfirmationStatus;

public record CreateConfirmationRequestResult(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        boolean created
) {
}
