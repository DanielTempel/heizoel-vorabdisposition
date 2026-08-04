package heizoel.backend.application.model.overview;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;

import java.time.Instant;
import java.time.LocalTime;

public record OrderOverviewItem(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        ConfirmationStatus confirmationStatus,
        Instant expiresAt
) {
}