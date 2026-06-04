package heizoel.backend.location.domain;

public record TrackingPreviewData(
        String deliveryAddress,
        Double locationX,
        Double locationY,
        Double targetLocationX,
        Double targetLocationY
) {
}
