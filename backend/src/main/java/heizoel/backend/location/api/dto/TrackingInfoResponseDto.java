package heizoel.backend.location.api.dto;

public record TrackingInfoResponseDto(
        boolean trackingAvailable,
        Double targetLocationX,
        Double targetLocationY
) {
}
