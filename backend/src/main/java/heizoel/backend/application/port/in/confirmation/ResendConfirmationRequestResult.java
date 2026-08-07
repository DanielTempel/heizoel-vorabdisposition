package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.domain.ConfirmationStatus;

public record ResendConfirmationRequestResult(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}