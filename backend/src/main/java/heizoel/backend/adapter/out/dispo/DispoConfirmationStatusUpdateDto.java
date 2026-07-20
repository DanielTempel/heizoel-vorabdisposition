package heizoel.backend.adapter.out.dispo;


import heizoel.backend.domain.ConfirmationStatus;

public record DispoConfirmationStatusUpdateDto(
        String externalOrderId,
        ConfirmationStatus confirmationStatus,
        String customerComment
) { }

