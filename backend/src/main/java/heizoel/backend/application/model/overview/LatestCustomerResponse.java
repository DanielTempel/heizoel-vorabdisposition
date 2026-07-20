package heizoel.backend.application.model.overview;

import heizoel.backend.domain.CustomerResponseType;

import java.time.Instant;

public record LatestCustomerResponse(
        CustomerResponseType responseType,
        String comment,
        Instant respondedAt
) {
}
