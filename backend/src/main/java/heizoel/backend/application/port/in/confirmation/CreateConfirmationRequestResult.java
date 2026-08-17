package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.domain.ConfirmationStatus;

public record CreateConfirmationRequestResult(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}
