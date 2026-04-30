package heizoel.backend.dispo.api.dto.response;

import heizoel.backend.dispo.domain.ConfirmationStatus;

public record DispoConfirmationResponseDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}