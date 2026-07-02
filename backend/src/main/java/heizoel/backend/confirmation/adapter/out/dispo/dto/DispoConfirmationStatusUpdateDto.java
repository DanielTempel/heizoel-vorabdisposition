package heizoel.backend.confirmation.adapter.out.dispo.dto;


import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

public record DispoConfirmationStatusUpdateDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) { }

