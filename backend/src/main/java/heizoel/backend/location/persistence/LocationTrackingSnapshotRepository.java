package heizoel.backend.location.persistence;

import heizoel.backend.location.domain.LocationTrackingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationTrackingSnapshotRepository extends JpaRepository<LocationTrackingSnapshot, Long> {

    Optional<LocationTrackingSnapshot> findByExternalOrderId(String externalOrderId);

    Optional<LocationTrackingSnapshot> findByConfirmationToken(String confirmationToken);
}
