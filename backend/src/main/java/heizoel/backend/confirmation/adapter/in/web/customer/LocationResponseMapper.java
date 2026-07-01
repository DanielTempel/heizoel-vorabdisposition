package heizoel.backend.confirmation.adapter.in.web.customer;

import heizoel.backend.confirmation.adapter.in.web.customer.dto.DriverLocationResponseDto;
import heizoel.backend.confirmation.adapter.in.web.customer.dto.TrackingInfoResponseDto;
import heizoel.backend.confirmation.domain.model.GeoCoordinate;
import org.springframework.stereotype.Component;

@Component
public class LocationResponseMapper {

    public DriverLocationResponseDto toDriverLocationResponse(GeoCoordinate coordinate) {
        return new DriverLocationResponseDto(
                coordinate.longitude(),
                coordinate.latitude()
        );
    }

    public TrackingInfoResponseDto toTrackingInfoResponse(
            boolean trackingAvailable,
            GeoCoordinate targetCoordinate
    ) {
        return new TrackingInfoResponseDto(
                trackingAvailable,
                targetCoordinate != null ? targetCoordinate.longitude() : null,
                targetCoordinate != null ? targetCoordinate.latitude() : null
        );
    }
}

