package heizoel.backend.application.port.out.location;

import heizoel.backend.application.model.GeoCoordinate;

import java.util.Optional;

public interface LocationTrackingService {

    Optional<GeoCoordinate> getDriverLocation(String externalOrderId);
}

