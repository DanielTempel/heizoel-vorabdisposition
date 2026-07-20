package heizoel.backend.application.model.overview;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmationOverviewItem(
        String externalOrderId,
        String customerName,
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        ConfirmationStatus confirmationStatus,
        Instant expiresAt
) {
}