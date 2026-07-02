package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.customer.DriverLocationResult;
import heizoel.backend.confirmation.application.port.in.customer.GetDriverLocationUseCase;
import heizoel.backend.confirmation.application.port.out.location.LocationTrackingService;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetDriverLocationUseCaseImpl implements GetDriverLocationUseCase {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
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
                .getDriverLocation(confirmationRequest.getOrderSnapshot().getExternalOrderId())
                .map(DriverLocationResult::new);
    }
}
