package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.confirmation.domain.model.GeoCoordinate;

public record DriverLocationResult(GeoCoordinate coordinate) {
}
