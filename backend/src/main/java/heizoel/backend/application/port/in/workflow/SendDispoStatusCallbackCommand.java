package heizoel.backend.application.port.in.workflow;

import heizoel.backend.domain.ConfirmationStatus;

public record SendDispoStatusCallbackCommand(
        Long confirmationRequestId,
        Long orderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
