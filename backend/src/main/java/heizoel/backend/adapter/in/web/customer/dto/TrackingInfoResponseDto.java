package heizoel.backend.adapter.in.web.customer.dto;

public record TrackingInfoResponseDto(
        boolean trackingAvailable,
        Double targetLocationX,
        Double targetLocationY
) {
}

