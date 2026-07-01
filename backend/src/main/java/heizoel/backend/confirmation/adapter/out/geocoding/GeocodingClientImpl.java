package heizoel.backend.confirmation.adapter.out.geocoding;

import heizoel.backend.confirmation.application.port.out.GeocodingClient;
import heizoel.backend.confirmation.adapter.out.tracking.GeoCoordinateMapper;
import heizoel.backend.confirmation.adapter.out.geocoding.RemoteCallExecutor;
import heizoel.backend.confirmation.domain.model.GeoCoordinate;
import heizoel.backend.confirmation.infrastructure.properties.LocationGeocodingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
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
    private final RemoteCallExecutor remoteCallExecutor;
    private final GeoCoordinateMapper geoCoordinateMapper;

    @Override
    public Optional<GeoCoordinate> geocode(String normalizedAddress) {
        if (!properties.isEnabled() || !StringUtils.hasText(normalizedAddress)) {
            return Optional.empty();
        }

        return remoteCallExecutor.execute(
                        () -> fetchSearchResults(normalizedAddress),
                        exception -> log.warn("Geocoding request failed for address={}", normalizedAddress, exception)
                )
                .flatMap(this::toFirstCoordinate);
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

    private NominatimSearchResult[] fetchSearchResults(String normalizedAddress) {
        return restClientBuilder.build()
                .get()
                .uri(buildSearchUri(normalizedAddress))
                .accept(MediaType.APPLICATION_JSON)
                .header("User-Agent", properties.getUserAgent())
                .header("Accept-Language", properties.getAcceptLanguage())
                .retrieve()
                .body(NominatimSearchResult[].class);
    }

    private Optional<GeoCoordinate> toFirstCoordinate(NominatimSearchResult[] response) {
        if (response.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(response)
                .findFirst()
                .flatMap(this::toCoordinate);
    }

    private Optional<GeoCoordinate> toCoordinate(NominatimSearchResult result) {
        Optional<GeoCoordinate> coordinate = geoCoordinateMapper.fromStrings(result.lon(), result.lat());
        if (coordinate.isEmpty()) {
            log.warn("Geocoding returned invalid coordinates lat={} lon={}", result.lat(), result.lon());
        }

        return coordinate;
    }

    private record NominatimSearchResult(
            String lat,
            String lon
    ) {
    }
}

