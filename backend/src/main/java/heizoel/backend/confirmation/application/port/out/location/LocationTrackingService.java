package heizoel.backend.confirmation.application.port.out.location;

import heizoel.backend.domain.model.GeoCoordinate;

import java.util.Optional;

public interface LocationTrackingService {

    Optional<GeoCoordinate> getDriverLocation(String externalOrderId);
}

