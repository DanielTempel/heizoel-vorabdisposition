package heizoel.backend.application.service.tracking;

import heizoel.backend.application.port.in.tracking.DriverLocationResult;
import heizoel.backend.application.port.in.tracking.GetDriverLocationUseCase;
import heizoel.backend.application.port.out.location.LocationTrackingService;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
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
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));

        if (!confirmationRequest.getDeliveryDate().isEqual(LocalDate.now())) {
            return Optional.empty();
        }

        return locationTrackingService
                .getDriverLocation(confirmationRequest.getOrder().getExternalOrderId())
                .map(DriverLocationResult::new);
    }
}
