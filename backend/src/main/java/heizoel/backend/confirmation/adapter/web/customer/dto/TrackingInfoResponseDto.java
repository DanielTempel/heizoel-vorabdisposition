package heizoel.backend.confirmation.adapter.web.customer.dto;

public record TrackingInfoResponseDto(
        boolean trackingAvailable,
        Double targetLocationX,
        Double targetLocationY
) {
}

