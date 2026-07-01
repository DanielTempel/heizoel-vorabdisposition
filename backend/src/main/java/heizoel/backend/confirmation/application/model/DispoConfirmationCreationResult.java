package heizoel.backend.confirmation.application.model;


import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;

public record DispoConfirmationCreationResult(
        DispoConfirmationResponseDto response,
        boolean created
) {
}
