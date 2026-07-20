package heizoel.backend.adapter.in.web.dispo.dto;

import heizoel.backend.domain.ConfirmationStatus;

public record DispoConfirmationResponseDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}
