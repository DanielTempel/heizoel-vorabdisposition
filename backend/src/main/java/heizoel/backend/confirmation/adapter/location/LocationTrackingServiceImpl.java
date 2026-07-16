package heizoel.backend.confirmation.adapter.location;

import heizoel.backend.infrastructure.properties.ConfirmationProperties;
import heizoel.backend.confirmation.application.port.out.location.LocationTrackingService;
import heizoel.backend.domain.model.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingServiceImpl implements LocationTrackingService {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

    @Override
    public Optional<GeoCoordinate> getDriverLocation(String externalOrderId) {
        if (!StringUtils.hasText(properties.getDispoTrackingUrl())
                || !StringUtils.hasText(externalOrderId)) {
            return Optional.empty();
        }

        try {
            DispoDriverLocationResponse response =
                    fetchDriverLocationResponse(externalOrderId);

            return Optional.ofNullable(response)
                    .map(this::toCoordinate);
        } catch (RestClientException exception) {
            log.warn(
                    "Driver location request failed for externalOrderId={}",
                    externalOrderId,
                    exception
            );
            return Optional.empty();
        }
    }

    private DispoDriverLocationResponse fetchDriverLocationResponse(String externalOrderId) {
        return restClient.get()
                .uri(
                        properties.getDispoTrackingUrl() + "/{externalOrderId}/driver-location",
                        externalOrderId
                )
                .retrieve()
                .body(DispoDriverLocationResponse.class);
    }

    private GeoCoordinate toCoordinate(DispoDriverLocationResponse response) {
        return new GeoCoordinate(
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