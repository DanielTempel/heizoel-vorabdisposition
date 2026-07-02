package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.confirmation.domain.model.GeoCoordinate;

import java.util.Optional;

public record TrackingInfoResult(
        boolean trackingAvailable,
        Optional<GeoCoordinate> targetCoordinate
) {
}
