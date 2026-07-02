package heizoel.backend.confirmation.application.port.in.customer;

import java.util.Optional;

public interface GetDriverLocationUseCase {

    Optional<DriverLocationResult> getDriverLocation(String token);
}
