package heizoel.backend.confirmation.adapter.web.dispo.dto;


import heizoel.backend.domain.model.enumeration.ConfirmationStatus;

public record DispoConfirmationStatusUpdateDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) { }

