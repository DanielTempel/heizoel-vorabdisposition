package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public record CreateConfirmationRequestResult(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        boolean created
) {
}
