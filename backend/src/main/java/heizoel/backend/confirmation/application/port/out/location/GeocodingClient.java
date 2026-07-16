package heizoel.backend.confirmation.application.port.out.location;

import heizoel.backend.domain.model.GeoCoordinate;

import java.util.Optional;

public interface GeocodingClient {

    Optional<GeoCoordinate> geocode(String normalizedAddress);
}

