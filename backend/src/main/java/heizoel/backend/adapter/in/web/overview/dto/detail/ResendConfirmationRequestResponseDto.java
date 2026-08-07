package heizoel.backend.adapter.in.web.overview.dto.detail;

import heizoel.backend.domain.ConfirmationStatus;

public record ResendConfirmationRequestResponseDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus
) {
}