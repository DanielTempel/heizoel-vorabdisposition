package heizoel.backend.confirmation.infrastructure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.location.geocoding")
public class LocationGeocodingProperties {

    private boolean enabled = true;
    private String provider = "nominatim";
    private String baseUrl = "https://nominatim.openstreetmap.org";
    private String searchPath = "/search";
    private String userAgent = "heizoel-vorabdisposition/1.0";
    private String email;
    private String countryCode = "de";
    private String acceptLanguage = "de,en";
    private int resultLimit = 1;
    private long cacheTtlMinutes = 60L;
}

