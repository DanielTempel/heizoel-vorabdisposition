package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.customer.GetTrackingInfoUseCase;
import heizoel.backend.confirmation.application.port.in.customer.TrackingInfoResult;
import heizoel.backend.confirmation.application.port.out.location.DeliveryAddressCoordinateResolver;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.domain.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.GeoCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetTrackingInfoUseCaseImpl implements GetTrackingInfoUseCase {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final DeliveryAddressCoordinateResolver deliveryAddressCoordinateResolver;

    @Override
    @Transactional(readOnly = true)
    public TrackingInfoResult getTrackingInfo(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));

        boolean trackingAvailable = confirmationRequest.getDeliveryDate().isEqual(LocalDate.now());

        Optional<GeoCoordinate> targetCoordinate = trackingAvailable
                ? deliveryAddressCoordinateResolver.resolve(
                confirmationRequest.getOrderSnapshot().getDeliveryAddress()
        )
                : Optional.empty();

        return new TrackingInfoResult(
                trackingAvailable,
                targetCoordinate
        );
    }


}
