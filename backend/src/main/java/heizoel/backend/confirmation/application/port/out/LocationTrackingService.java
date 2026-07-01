package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.GeoCoordinate;

import java.util.Optional;

public interface LocationTrackingService {

    Optional<GeoCoordinate> getDriverLocation(String externalOrderId);
}

