package heizoel.backend.confirmation.adapter.in.web.customer;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.adapter.in.web.customer.dto.DriverLocationResponseDto;
import heizoel.backend.confirmation.application.port.out.LocationTrackingService;
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

    private final ConfirmationRequestResolver confirmationRequestResolver;
    private final LocationTrackingService locationTrackingService;
    private final LocationResponseMapper locationResponseMapper;

    @GetMapping("/{token}/driver-location")
    @Transactional(readOnly = true)
    public ResponseEntity<DriverLocationResponseDto> getDriverLocation(
            @PathVariable String token
    ) {
        ConfirmationRequest confirmationRequest = confirmationRequestResolver.resolveByToken(token);

        if (!confirmationRequest.getDeliveryDate().isEqual(LocalDate.now())) {
            return ResponseEntity.notFound().build();
        }

        return locationTrackingService
                .getDriverLocation(confirmationRequest.getOrderSnapshot().getExternalOrderId())
                .map(locationResponseMapper::toDriverLocationResponse)
                .map(response -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

