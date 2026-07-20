package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.application.model.overview.ConfirmationOverviewItem;

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
    public static DashboardOrderResponseDto from(ConfirmationOverviewItem row) {
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
