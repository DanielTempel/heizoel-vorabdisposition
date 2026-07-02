package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public record SendDispoStatusCallbackCommand(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
