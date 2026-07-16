package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.domain.model.GeoCoordinate;

import java.util.Optional;

public record TrackingInfoResult(
        boolean trackingAvailable,
        Optional<GeoCoordinate> targetCoordinate
) {
}
