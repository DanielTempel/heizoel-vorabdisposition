package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.application.model.overview.LatestConfirmationRequest;

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
    public static DashboardLatestRequestResponseDto from(LatestConfirmationRequest request) {
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