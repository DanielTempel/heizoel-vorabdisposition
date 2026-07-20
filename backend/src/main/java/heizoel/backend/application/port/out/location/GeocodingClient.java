package heizoel.backend.application.port.out.location;

import heizoel.backend.application.model.GeoCoordinate;

import java.util.Optional;

public interface GeocodingClient {

    Optional<GeoCoordinate> geocode(String normalizedAddress);
}

