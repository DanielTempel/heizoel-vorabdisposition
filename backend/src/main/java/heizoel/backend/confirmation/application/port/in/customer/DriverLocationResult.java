package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.domain.model.GeoCoordinate;

public record DriverLocationResult(GeoCoordinate coordinate) {
}
