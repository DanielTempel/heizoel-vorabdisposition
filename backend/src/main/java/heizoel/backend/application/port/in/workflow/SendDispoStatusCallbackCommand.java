package heizoel.backend.application.port.in.workflow;

import heizoel.backend.domain.ConfirmationStatus;

public record SendDispoStatusCallbackCommand(
        Long orderSnapshotId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
