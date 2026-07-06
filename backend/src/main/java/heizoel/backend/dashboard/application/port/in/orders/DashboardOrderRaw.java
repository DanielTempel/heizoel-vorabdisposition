package heizoel.backend.dashboard.application.port.in.orders;

import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardOrderRaw(
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