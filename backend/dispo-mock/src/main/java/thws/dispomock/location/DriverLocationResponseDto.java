package thws.dispomock.location;

import java.time.Instant;

public record DriverLocationResponseDto(
        String externalOrderId,
        double locationX,
        double locationY,
        Instant capturedAt
) {
}
