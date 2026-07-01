package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.GeoCoordinate;

import java.util.Optional;

public interface DeliveryAddressCoordinateResolver {

    Optional<GeoCoordinate> resolve(String deliveryAddress);
}

