package heizoel.backend.dispo.application.model.command;

import heizoel.backend.dispo.domain.ConfirmationStatus;

public record CustomerConfirmationStatusChangedEvent(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) {
}