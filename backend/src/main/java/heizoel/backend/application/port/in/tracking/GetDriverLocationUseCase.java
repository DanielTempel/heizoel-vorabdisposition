package heizoel.backend.application.port.in.tracking;

import java.util.Optional;

public interface GetDriverLocationUseCase {

    Optional<DriverLocationResult> getDriverLocation(String token);
}
