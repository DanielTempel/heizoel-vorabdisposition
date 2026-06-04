package heizoel.backend.location.application.dto;

public record IncomingTrackingPayload(
        String externalOrderId,
        String deliveryAddress,
        Double locationX,
        Double locationY,
        Double targetLocationX,
        Double targetLocationY
) {
}
