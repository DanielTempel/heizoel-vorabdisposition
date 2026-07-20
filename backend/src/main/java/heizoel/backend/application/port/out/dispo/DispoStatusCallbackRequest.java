package heizoel.backend.application.port.out.dispo;

import heizoel.backend.domain.ConfirmationStatus;

public record DispoStatusCallbackRequest(
        String callbackUrl,
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
