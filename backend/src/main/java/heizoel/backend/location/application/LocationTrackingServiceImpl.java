package heizoel.backend.location.application;

import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.application.mapper.GeoCoordinateMapper;
import heizoel.backend.location.application.support.RemoteCallExecutor;
import heizoel.backend.location.domain.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationTrackingServiceImpl implements LocationTrackingService {

    private final RestClient restClient;
    private final ConfirmationProperties properties;
    private final RemoteCallExecutor remoteCallExecutor;
    private final GeoCoordinateMapper geoCoordinateMapper;

    @Override
    public Optional<GeoCoordinate> getDriverLocation(String externalOrderId) {
        if (!StringUtils.hasText(properties.getDispoTrackingUrl()) || !StringUtils.hasText(externalOrderId)) {
            return Optional.empty();
        }

        return remoteCallExecutor.execute(() -> fetchDriverLocationResponse(externalOrderId))
                .map(this::toCoordinate);
    }

    private DispoDriverLocationResponse fetchDriverLocationResponse(String externalOrderId) {
        return restClient.get()
                .uri(properties.getDispoTrackingUrl() + "/{externalOrderId}/driver-location", externalOrderId)
                .retrieve()
                .body(DispoDriverLocationResponse.class);
    }

    private GeoCoordinate toCoordinate(DispoDriverLocationResponse response) {
        return geoCoordinateMapper.fromNumbers(
                response.locationX(),
                response.locationY()
        );
    }

    private record DispoDriverLocationResponse(
            String externalOrderId,
            double locationX,
            double locationY
    ) {
    }
}
