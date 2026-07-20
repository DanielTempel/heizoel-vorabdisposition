package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.application.model.overview.LatestCustomerResponse;

import java.time.Instant;

public record DashboardLatestCustomerResponseDto(
        CustomerResponseType responseType,
        String comment,
        Instant receivedAt
) {
    public static DashboardLatestCustomerResponseDto from(LatestCustomerResponse response) {
        return new DashboardLatestCustomerResponseDto(
                response.responseType(),
                response.comment(),
                response.respondedAt()
        );
    }
}