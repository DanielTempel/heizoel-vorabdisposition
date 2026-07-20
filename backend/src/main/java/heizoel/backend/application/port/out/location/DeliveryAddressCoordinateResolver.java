package heizoel.backend.application.port.out.location;

import heizoel.backend.application.model.GeoCoordinate;

import java.util.Optional;

public interface DeliveryAddressCoordinateResolver {

    Optional<GeoCoordinate> resolve(String deliveryAddress);
}

