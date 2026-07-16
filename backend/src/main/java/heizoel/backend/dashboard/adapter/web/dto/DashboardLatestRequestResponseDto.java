package heizoel.backend.dashboard.adapter.web.dto;

import heizoel.backend.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.dashboard.application.port.in.orderdetail.DashboardLatestRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardLatestRequestResponseDto(
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        Instant sentAt,
        Instant expiresAt,
        boolean active
) {
    public static DashboardLatestRequestResponseDto from(DashboardLatestRequest request) {
        return new DashboardLatestRequestResponseDto(
                request.deliveryDate(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.communicationChannel(),
                request.sentAt(),
                request.expiresAt(),
                request.active()
        );
    }
}