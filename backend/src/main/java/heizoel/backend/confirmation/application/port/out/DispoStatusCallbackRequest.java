package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.ConfirmationStatus;

public record DispoStatusCallbackRequest(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
