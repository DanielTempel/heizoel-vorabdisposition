package heizoel.backend.location.application;

import heizoel.backend.location.application.interfaces.DriverLocationService;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.domain.DriverLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationTrackingServiceImpl implements LocationTrackingService {

    private final DriverLocationService driverLocationService;

    @Override
    public Optional<DriverLocation> getDriverLocation(String externalOrderId) {
        return driverLocationService.getDriverLocation(externalOrderId);
    }
}
