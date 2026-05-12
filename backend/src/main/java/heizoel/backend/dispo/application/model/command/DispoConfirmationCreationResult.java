package heizoel.backend.dispo.application.model.command;


import heizoel.backend.dispo.api.dto.response.DispoConfirmationResponseDto;

public record DispoConfirmationCreationResult(
        DispoConfirmationResponseDto response,
        boolean created
) {
}