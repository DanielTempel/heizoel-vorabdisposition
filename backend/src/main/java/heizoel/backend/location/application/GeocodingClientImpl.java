package heizoel.backend.location.application;

import heizoel.backend.location.application.interfaces.GeocodingClient;
import heizoel.backend.location.domain.GeoCoordinate;
import heizoel.backend.location.infrastructure.LocationGeocodingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingClientImpl implements GeocodingClient {

    private final RestClient.Builder restClientBuilder;
    private final LocationGeocodingProperties properties;

    @Override
    public Optional<GeoCoordinate> geocode(String normalizedAddress) {
        if (!properties.isEnabled() || normalizedAddress == null || normalizedAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            NominatimSearchResult[] response = restClientBuilder.build()
                    .get()
                    .uri(buildSearchUri(normalizedAddress))
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", properties.getUserAgent())
                    .header("Accept-Language", properties.getAcceptLanguage())
                    .retrieve()
                    .body(NominatimSearchResult[].class);

            if (response == null || response.length == 0) {
                return Optional.empty();
            }

            return Arrays.stream(response)
                    .findFirst()
                    .flatMap(this::toCoordinate);
        } catch (RestClientException exception) {
            log.warn("Geocoding request failed for address={}", normalizedAddress, exception);
            return Optional.empty();
        }
    }

    private URI buildSearchUri(String normalizedAddress) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getBaseUrl())
                .path(properties.getSearchPath())
                .queryParam("q", normalizedAddress)
                .queryParam("format", "jsonv2")
                .queryParam("limit", properties.getResultLimit())
                .queryParam("addressdetails", 1)
                .queryParam("countrycodes", properties.getCountryCode());

        if (properties.getEmail() != null && !properties.getEmail().isBlank()) {
            builder.queryParam("email", properties.getEmail());
        }

        return builder.encode().build().toUri();
    }

    private Optional<GeoCoordinate> toCoordinate(NominatimSearchResult result) {
        try {
            return Optional.of(new GeoCoordinate(
                    Double.parseDouble(result.lon()),
                    Double.parseDouble(result.lat())
            ));
        } catch (NumberFormatException exception) {
            log.warn("Geocoding returned invalid coordinates lat={} lon={}", result.lat(), result.lon());
            return Optional.empty();
        }
    }

    private record NominatimSearchResult(
            String lat,
            String lon
    ) {
    }
}
