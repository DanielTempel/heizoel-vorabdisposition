package heizoel.backend.location.application;

import heizoel.backend.location.application.interfaces.DeliveryAddressCoordinateResolver;
import heizoel.backend.location.application.interfaces.GeocodingClient;
import heizoel.backend.location.domain.GeoCoordinate;
import heizoel.backend.location.infrastructure.LocationGeocodingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAddressCoordinateResolverImpl implements DeliveryAddressCoordinateResolver {

    private static final GeoCoordinate DEFAULT_CENTER = new GeoCoordinate(9.9534D, 49.7913D);

    private final GeocodingClient geocodingClient;
    private final AddressNormalizer addressNormalizer;
    private final LocationGeocodingProperties properties;

    private final ConcurrentMap<String, CachedCoordinate> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<GeoCoordinate> resolve(String deliveryAddress) {
        Instant now = Instant.now();
        String cacheKey = addressNormalizer.cacheKey(deliveryAddress);
        String geocodingQuery = addressNormalizer.toGeocodingQuery(deliveryAddress);

        if (cacheKey.isBlank() || geocodingQuery.isBlank()) {
            return Optional.empty();
        }

        CachedCoordinate cachedCoordinate = cache.compute(cacheKey, (key, existing) -> {
            if (existing != null && existing.expiresAt().isAfter(now)) {
                return existing;
            }

            Optional<GeoCoordinate> coordinate = resolveCoordinate(cacheKey, geocodingQuery);
            return new CachedCoordinate(coordinate, now.plusSeconds(properties.getCacheTtlMinutes() * 60));
        });

        return cachedCoordinate.coordinate();
    }

    private Optional<GeoCoordinate> resolveCoordinate(String cacheKey, String geocodingQuery) {
        Optional<GeoCoordinate> geocodedCoordinate = geocodingClient.geocode(geocodingQuery);
        if (geocodedCoordinate.isPresent()) {
            return geocodedCoordinate;
        }

        GeoCoordinate fallbackCoordinate = estimateCoordinate(cacheKey);
        log.warn("Falling back to estimated coordinate for address={}", geocodingQuery);
        return Optional.of(fallbackCoordinate);
    }

    private GeoCoordinate estimateCoordinate(String cacheKey) {
        long unsignedHash = Integer.toUnsignedLong(cacheKey.hashCode());
        double longitudeOffset = (((unsignedHash % 1601L) - 800L) / 10000.0D);
        double latitudeOffset = ((((unsignedHash / 1601L) % 1601L) - 800L) / 10000.0D);

        return new GeoCoordinate(
                DEFAULT_CENTER.longitude() + longitudeOffset,
                DEFAULT_CENTER.latitude() + latitudeOffset
        );
    }

    private record CachedCoordinate(
            Optional<GeoCoordinate> coordinate,
            Instant expiresAt
    ) {
    }
}
