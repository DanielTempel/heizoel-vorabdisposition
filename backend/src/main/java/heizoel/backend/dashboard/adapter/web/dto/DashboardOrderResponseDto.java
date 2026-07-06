package heizoel.backend.dashboard.adapter.web.dto;

import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.dashboard.application.port.in.orders.DashboardOrderRaw;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardOrderResponseDto(
        String externalOrderId,
        String customerName,
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        ConfirmationStatus confirmationStatus,
        Instant expiresAt
) {
    public static DashboardOrderResponseDto from(DashboardOrderRaw row) {
        return new DashboardOrderResponseDto(
                row.externalOrderId(),
                row.customerName(),
                row.deliveryDate(),
                row.deliveryWindowStart(),
                row.deliveryWindowEnd(),
                row.communicationChannel(),
                row.confirmationStatus(),
                row.expiresAt()
        );
    }
}
