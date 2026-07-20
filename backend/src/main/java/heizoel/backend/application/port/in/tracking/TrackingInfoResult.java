package heizoel.backend.application.port.in.tracking;

import heizoel.backend.application.model.GeoCoordinate;

import java.util.Optional;

public record TrackingInfoResult(
        boolean trackingAvailable,
        Optional<GeoCoordinate> targetCoordinate
) {
}
