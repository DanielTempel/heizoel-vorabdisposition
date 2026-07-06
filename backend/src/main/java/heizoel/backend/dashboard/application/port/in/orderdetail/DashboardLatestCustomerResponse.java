package heizoel.backend.dashboard.application.port.in.orderdetail;

import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;

import java.time.Instant;

public record DashboardLatestCustomerResponse(
        CustomerResponseType responseType,
        String comment,
        Instant respondedAt
) {
}
