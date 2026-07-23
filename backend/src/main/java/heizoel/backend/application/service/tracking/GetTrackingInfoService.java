package heizoel.backend.application.service.tracking;

import heizoel.backend.application.port.in.tracking.GetTrackingInfoUseCase;
import heizoel.backend.application.port.in.tracking.TrackingInfoResult;
import heizoel.backend.application.port.out.location.DeliveryAddressCoordinateResolver;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.application.model.GeoCoordinate;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetTrackingInfoService implements GetTrackingInfoUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
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
                confirmationRequest.getOrder().getDeliveryAddress()
        )
                : Optional.empty();

        return new TrackingInfoResult(
                trackingAvailable,
                targetCoordinate
        );
    }


}
