package heizoel.backend.location.application.dto;

public record TrackingTokenBinding(
        String externalOrderId,
        String confirmationToken
) {
}
