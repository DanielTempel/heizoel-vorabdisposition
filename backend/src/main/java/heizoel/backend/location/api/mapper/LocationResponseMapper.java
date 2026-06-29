package heizoel.backend.location.api.mapper;

import heizoel.backend.location.api.dto.DriverLocationResponseDto;
import heizoel.backend.location.api.dto.TrackingInfoResponseDto;
import heizoel.backend.location.domain.GeoCoordinate;
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
