package heizoel.backend.location.application;

import heizoel.backend.location.application.dto.IncomingTrackingPayload;
import heizoel.backend.location.application.dto.TrackingTokenBinding;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.domain.LocationTrackingSnapshot;
import heizoel.backend.location.domain.TrackingPreviewData;
import heizoel.backend.location.persistence.ConfirmationRequestLookupRepository;
import heizoel.backend.location.persistence.LocationTrackingSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationTrackingServiceImpl implements LocationTrackingService {

    private final LocationTrackingSnapshotRepository snapshotRepository;
    private final ConfirmationRequestLookupRepository confirmationRequestLookupRepository;

    @Override
    @Transactional
    public void captureConfirmationRequest(IncomingTrackingPayload payload) {
        LocationTrackingSnapshot snapshot = snapshotRepository.findByExternalOrderId(payload.externalOrderId())
                .orElseGet(LocationTrackingSnapshot::new);

        snapshot.setExternalOrderId(payload.externalOrderId());
        snapshot.setDeliveryAddress(payload.deliveryAddress());
        snapshot.setLocationX(payload.locationX());
        snapshot.setLocationY(payload.locationY());
        snapshot.setTargetLocationX(payload.targetLocationX());
        snapshot.setTargetLocationY(payload.targetLocationY());
        snapshot.setUpdatedAt(Instant.now());

        Optional<TrackingTokenBinding> tokenBinding =
                confirmationRequestLookupRepository.findLatestByExternalOrderId(payload.externalOrderId());
        tokenBinding.map(TrackingTokenBinding::confirmationToken).ifPresent(snapshot::setConfirmationToken);

        snapshotRepository.save(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrackingPreviewData> findTrackingPreviewByToken(String token) {
        return snapshotRepository.findByConfirmationToken(token)
                .map(snapshot -> new TrackingPreviewData(
                        snapshot.getDeliveryAddress(),
                        snapshot.getLocationX(),
                        snapshot.getLocationY(),
                        snapshot.getTargetLocationX(),
                        snapshot.getTargetLocationY()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocationTrackingSnapshot> findByExternalOrderId(String externalOrderId) {
        return snapshotRepository.findByExternalOrderId(externalOrderId);
    }

    @Override
    @Transactional
    public LocationTrackingSnapshot save(LocationTrackingSnapshot snapshot) {
        snapshot.setUpdatedAt(Instant.now());
        return snapshotRepository.save(snapshot);
    }
}
