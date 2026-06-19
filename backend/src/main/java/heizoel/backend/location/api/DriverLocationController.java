package heizoel.backend.location.api;

import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.exceptions.customer.ConfirmationRequestNotFoundException;
import heizoel.backend.location.api.dto.DriverLocationResponseDto;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.domain.DriverLocation;
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
public class DriverLocationController {

    private final ConfirmationRequestService confirmationRequestService;
    private final LocationTrackingService locationTrackingService;

    @GetMapping("/{token}/driver-location")
    @Transactional(readOnly = true)
    public ResponseEntity<DriverLocationResponseDto> getDriverLocation(
            @PathVariable String token
    ) {
        ConfirmationRequest confirmationRequest = confirmationRequestService.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));

        if (!confirmationRequest.getDeliveryDate().isEqual(LocalDate.now())) {
            return ResponseEntity.notFound().build();
        }

        return locationTrackingService
                .getDriverLocation(confirmationRequest.getOrderSnapshot().getExternalOrderId())
                .map(this::toResponse)
                .map(response -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private DriverLocationResponseDto toResponse(DriverLocation driverLocation) {
        return new DriverLocationResponseDto(
                driverLocation.locationX(),
                driverLocation.locationY()
        );
    }
}
