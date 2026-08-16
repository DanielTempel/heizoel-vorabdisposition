package heizoel.backend.application.service.tracking;

import heizoel.backend.application.port.in.tracking.DriverLocationResult;
import heizoel.backend.application.port.in.tracking.GetDriverLocationUseCase;
import heizoel.backend.application.port.out.location.LocationTrackingService;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetDriverLocationService implements GetDriverLocationUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final LocationTrackingService locationTrackingService;

    @Override
    @Transactional(readOnly = true)
    public Optional<DriverLocationResult> getDriverLocation(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findLatestByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
        DeliverySlot deliverySlot = confirmationRequest.getDeliverySlot();

        if (!deliverySlot.getDate().isEqual(LocalDate.now())) {
            return Optional.empty();
        }

        return locationTrackingService
                .getDriverLocation(confirmationRequest.getOrder().getExternalOrderId())
                .map(DriverLocationResult::new);
    }
}
