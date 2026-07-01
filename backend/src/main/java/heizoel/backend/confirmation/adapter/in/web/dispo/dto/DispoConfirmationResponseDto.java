package heizoel.backend.confirmation.adapter.in.web.dispo.dto;

import heizoel.backend.confirmation.domain.model.ConfirmationStatus;

public record DispoConfirmationResponseDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}
