package heizoel.backend.location.application.interfaces;

import heizoel.backend.location.domain.DriverLocation;

import java.util.Optional;

public interface DriverLocationService {

    Optional<DriverLocation> getDriverLocation(String externalOrderId);
}
