package heizoel.backend.location.application.interfaces;

import heizoel.backend.location.application.dto.IncomingTrackingPayload;
import heizoel.backend.location.domain.LocationTrackingSnapshot;
import heizoel.backend.location.domain.TrackingPreviewData;

import java.util.Optional;

public interface LocationTrackingService {

    void captureConfirmationRequest(IncomingTrackingPayload payload);

    Optional<TrackingPreviewData> findTrackingPreviewByToken(String token);

    Optional<LocationTrackingSnapshot> findByExternalOrderId(String externalOrderId);

    LocationTrackingSnapshot save(LocationTrackingSnapshot snapshot);
}
