package heizoel.backend.adapter.in.web.customer;

import heizoel.backend.adapter.in.web.customer.dto.DriverLocationResponseDto;
import heizoel.backend.adapter.in.web.customer.dto.TrackingInfoResponseDto;
import heizoel.backend.application.port.in.tracking.DriverLocationResult;
import heizoel.backend.application.port.in.tracking.GetDriverLocationUseCase;
import heizoel.backend.application.port.in.tracking.GetTrackingInfoUseCase;
import heizoel.backend.application.port.in.tracking.TrackingInfoResult;
import heizoel.backend.application.model.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
@Slf4j
public class CustomerTrackingController {

    private final GetTrackingInfoUseCase getTrackingInfoUseCase;
    private final GetDriverLocationUseCase getDriverLocationUseCase;

    @GetMapping("/{token}/tracking-info")
    public ResponseEntity<TrackingInfoResponseDto> getTrackingInfo(
            @PathVariable String token
    ) {
        log.info("Started getTrackingInfo");
        TrackingInfoResult result = getTrackingInfoUseCase.getTrackingInfo(token);

        GeoCoordinate targetCoordinate = result.targetCoordinate().orElse(null);

        ResponseEntity<TrackingInfoResponseDto> response = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TrackingInfoResponseDto(
                        result.trackingAvailable(),
                        targetCoordinate != null ? targetCoordinate.longitude() : null,
                        targetCoordinate != null ? targetCoordinate.latitude() : null
                ));

        log.info(
                "Completed getTrackingInfo: trackingAvailable={}, targetCoordinatePresent={}, status=200",
                result.trackingAvailable(),
                targetCoordinate != null
        );
        return response;
    }

    @GetMapping("/{token}/driver-location")
    public ResponseEntity<DriverLocationResponseDto> getDriverLocation(
            @PathVariable String token
    ) {
        log.info("Started getDriverLocation");
        Optional<DriverLocationResult> locationResult =
                getDriverLocationUseCase.getDriverLocation(token);
        ResponseEntity<DriverLocationResponseDto> responseEntity = locationResult
                .map(result -> new DriverLocationResponseDto(
                        result.coordinate().longitude(),
                        result.coordinate().latitude()
                ))
                .map(response -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());

        log.info(
                "Completed getDriverLocation: locationFound={}, status={}",
                locationResult.isPresent(),
                responseEntity.getStatusCode().value()
        );
        return responseEntity;
    }
}
