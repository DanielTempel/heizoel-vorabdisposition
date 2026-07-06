package heizoel.backend.dashboard.application.port.in.orderdetail;

import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardLatestRequest(
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        Instant sentAt,
        Instant expiresAt,
        boolean active
) {
}