package heizoel.backend.dashboard.adapter.web.dto;

import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.dashboard.application.port.in.orderdetail.DashboardLatestCustomerResponse;

import java.time.Instant;

public record DashboardLatestCustomerResponseDto(
        CustomerResponseType responseType,
        String comment,
        Instant receivedAt
) {
    public static DashboardLatestCustomerResponseDto from(DashboardLatestCustomerResponse response) {
        return new DashboardLatestCustomerResponseDto(
                response.responseType(),
                response.comment(),
                response.respondedAt()
        );
    }
}