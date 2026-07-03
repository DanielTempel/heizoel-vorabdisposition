package heizoel.backend.confirmation.application.port.in.dispo;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public record SendDispoStatusCallbackCommand(
        Long orderSnapshotId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
