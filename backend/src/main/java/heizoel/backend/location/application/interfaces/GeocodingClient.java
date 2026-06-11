package heizoel.backend.location.application.interfaces;

import heizoel.backend.location.domain.GeoCoordinate;

import java.util.Optional;

public interface GeocodingClient {

    Optional<GeoCoordinate> geocode(String normalizedAddress);
}
