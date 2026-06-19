package heizoel.backend.location.domain;

public record DriverLocation(
        String externalOrderId,
        double locationX,
        double locationY
) {
}
