package heizoel.backend.location.application.interfaces;

import heizoel.backend.location.domain.GeoCoordinate;

import java.util.Optional;

public interface LocationTrackingService {

    Optional<GeoCoordinate> getDriverLocation(String externalOrderId);
}
