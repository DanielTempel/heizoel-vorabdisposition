package heizoel.backend.dispo.api.dto.response;


import heizoel.backend.dispo.domain.ConfirmationStatus;

public record DispoConfirmationStatusUpdateDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) { }
