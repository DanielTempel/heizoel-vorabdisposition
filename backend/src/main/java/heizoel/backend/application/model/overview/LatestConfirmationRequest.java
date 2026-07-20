package heizoel.backend.application.model.overview;

import heizoel.backend.domain.CommunicationChannel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record LatestConfirmationRequest(
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        Instant sentAt,
        Instant expiresAt,
        boolean active
) {
}