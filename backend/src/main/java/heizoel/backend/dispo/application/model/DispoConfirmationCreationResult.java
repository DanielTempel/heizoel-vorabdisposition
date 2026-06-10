package heizoel.backend.dispo.application.model;


import heizoel.backend.dispo.api.dto.response.DispoConfirmationResponseDto;

public record DispoConfirmationCreationResult(
        DispoConfirmationResponseDto response,
        boolean created
) {
}