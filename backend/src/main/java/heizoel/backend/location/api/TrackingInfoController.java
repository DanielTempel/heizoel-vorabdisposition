package heizoel.backend.location.api;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.location.api.dto.TrackingInfoResponseDto;
import heizoel.backend.location.api.mapper.LocationResponseMapper;
import heizoel.backend.location.api.resolver.ConfirmationRequestResolver;
import heizoel.backend.location.application.interfaces.DeliveryAddressCoordinateResolver;
import heizoel.backend.location.domain.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
public class TrackingInfoController {

    private final ConfirmationRequestResolver confirmationRequestResolver;
    private final DeliveryAddressCoordinateResolver deliveryAddressCoordinateResolver;
    private final LocationResponseMapper locationResponseMapper;

    @GetMapping("/{token}/tracking-info")
    @Transactional(readOnly = true)
    public ResponseEntity<TrackingInfoResponseDto> getTrackingInfo(
            @PathVariable String token
    ) {
        ConfirmationRequest confirmationRequest = confirmationRequestResolver.resolveByToken(token);

        boolean trackingAvailable = confirmationRequest.getDeliveryDate().isEqual(LocalDate.now());
        GeoCoordinate targetCoordinate = trackingAvailable
                ? deliveryAddressCoordinateResolver.resolve(
                        confirmationRequest.getOrderSnapshot().getDeliveryAddress()
                ).orElse(null)
                : null;

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(locationResponseMapper.toTrackingInfoResponse(
                        trackingAvailable,
                        targetCoordinate
                ));
    }
}
