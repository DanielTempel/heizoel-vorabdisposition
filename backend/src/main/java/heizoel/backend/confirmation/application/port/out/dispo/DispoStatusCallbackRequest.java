package heizoel.backend.confirmation.application.port.out.dispo;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public record DispoStatusCallbackRequest(
        String callbackUrl,
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
