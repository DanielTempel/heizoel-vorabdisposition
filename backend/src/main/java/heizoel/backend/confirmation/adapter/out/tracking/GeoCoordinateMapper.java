package heizoel.backend.confirmation.adapter.out.tracking;

import heizoel.backend.confirmation.domain.model.GeoCoordinate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GeoCoordinateMapper {

    public GeoCoordinate fromNumbers(double longitude, double latitude) {
        return new GeoCoordinate(longitude, latitude);
    }

    public Optional<GeoCoordinate> fromStrings(String longitude, String latitude) {
        try {
            return Optional.of(fromNumbers(
                    Double.parseDouble(longitude),
                    Double.parseDouble(latitude)
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}

