package heizoel.backend.dispo.api.dto.response;


import heizoel.backend.dispo.domain.ConfirmationStatus;

public record ConfirmationStatusUpdateDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) { }
