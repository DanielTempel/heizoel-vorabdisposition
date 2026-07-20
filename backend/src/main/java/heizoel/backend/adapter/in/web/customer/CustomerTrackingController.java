package heizoel.backend.adapter.in.web.customer;

import heizoel.backend.adapter.in.web.customer.dto.DriverLocationResponseDto;
import heizoel.backend.adapter.in.web.customer.dto.TrackingInfoResponseDto;
import heizoel.backend.application.port.in.tracking.GetDriverLocationUseCase;
import heizoel.backend.application.port.in.tracking.GetTrackingInfoUseCase;
import heizoel.backend.application.port.in.tracking.TrackingInfoResult;
import heizoel.backend.application.model.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
public class CustomerTrackingController {

    private final GetTrackingInfoUseCase getTrackingInfoUseCase;
    private final GetDriverLocationUseCase getDriverLocationUseCase;

    @GetMapping("/{token}/tracking-info")
    public ResponseEntity<TrackingInfoResponseDto> getTrackingInfo(
            @PathVariable String token
    ) {
        TrackingInfoResult result = getTrackingInfoUseCase.getTrackingInfo(token);

        GeoCoordinate targetCoordinate = result.targetCoordinate().orElse(null);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TrackingInfoResponseDto(
                        result.trackingAvailable(),
                        targetCoordinate != null ? targetCoordinate.longitude() : null,
                        targetCoordinate != null ? targetCoordinate.latitude() : null
                ));
    }

    @GetMapping("/{token}/driver-location")
    public ResponseEntity<DriverLocationResponseDto> getDriverLocation(
            @PathVariable String token
    ) {
        return getDriverLocationUseCase.getDriverLocation(token)
                .map(result -> new DriverLocationResponseDto(
                        result.coordinate().longitude(),
                        result.coordinate().latitude()
                ))
                .map(response -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
