package heizoel.backend.location.application;

import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.location.application.interfaces.DriverLocationService;
import heizoel.backend.location.domain.DriverLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl implements DriverLocationService {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

    @Override
    public Optional<DriverLocation> getDriverLocation(String externalOrderId) {
        if (properties.getDispoTrackingUrl() == null || properties.getDispoTrackingUrl().isBlank()) {
            return Optional.empty();
        }

        try {
            DispoDriverLocationResponse response = restClient.get()
                    .uri(properties.getDispoTrackingUrl() + "/{externalOrderId}/driver-location", externalOrderId)
                    .retrieve()
                    .body(DispoDriverLocationResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(new DriverLocation(
                    response.externalOrderId(),
                    response.locationX(),
                    response.locationY()
            ));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private record DispoDriverLocationResponse(
            String externalOrderId,
            double locationX,
            double locationY
    ) {
    }
}
