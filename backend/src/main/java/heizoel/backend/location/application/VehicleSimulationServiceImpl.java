package heizoel.backend.location.application;

import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.application.interfaces.VehicleSimulationService;
import heizoel.backend.location.domain.LocationTrackingSnapshot;
import heizoel.backend.location.domain.VehicleSimulationStartResult;
import heizoel.backend.location.domain.VehicleSimulationStatus;
import heizoel.backend.location.infrastructure.VehicleSimulationProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleSimulationServiceImpl implements VehicleSimulationService {

    private final LocationTrackingService locationTrackingService;
    private final VehicleSimulationProperties properties;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, ScheduledFuture<?>> activeSimulations = new ConcurrentHashMap<>();

    @Override
    public synchronized VehicleSimulationStartResult startSimulation(String externalOrderId) {
        LocationTrackingSnapshot snapshot = locationTrackingService.findByExternalOrderId(externalOrderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tracking snapshot was not found."
                ));

        if (hasReachedDestination(snapshot)) {
            return new VehicleSimulationStartResult(externalOrderId, VehicleSimulationStatus.COMPLETED);
        }

        ScheduledFuture<?> activeSimulation = activeSimulations.get(externalOrderId);
        if (activeSimulation != null && !activeSimulation.isDone()) {
            return new VehicleSimulationStartResult(externalOrderId, VehicleSimulationStatus.ALREADY_RUNNING);
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> advanceVehicle(externalOrderId),
                0L,
                properties.getTickIntervalMillis(),
                TimeUnit.MILLISECONDS
        );
        activeSimulations.put(externalOrderId, future);

        return new VehicleSimulationStartResult(externalOrderId, VehicleSimulationStatus.STARTED);
    }

    private void advanceVehicle(String externalOrderId) {
        try {
            Optional<LocationTrackingSnapshot> optionalSnapshot =
                    locationTrackingService.findByExternalOrderId(externalOrderId);

            if (optionalSnapshot.isEmpty()) {
                stopSimulation(externalOrderId);
                return;
            }

            LocationTrackingSnapshot snapshot = optionalSnapshot.get();
            if (hasReachedDestination(snapshot)) {
                snapshot.setLocationX(snapshot.getTargetLocationX());
                snapshot.setLocationY(snapshot.getTargetLocationY());
                locationTrackingService.save(snapshot);
                stopSimulation(externalOrderId);
                return;
            }

            double remainingKilometers = distanceInKilometers(
                    snapshot.getLocationY(),
                    snapshot.getLocationX(),
                    snapshot.getTargetLocationY(),
                    snapshot.getTargetLocationX()
            );

            double nextStepKilometers = Math.min(
                    Math.max(remainingKilometers * 0.14D, properties.getMinimumStepKilometers()),
                    properties.getMaximumStepKilometers()
            );
            double stepRatio = Math.min(
                    nextStepKilometers / remainingKilometers,
                    properties.getMaximumStepRatio()
            );

            snapshot.setLocationX(interpolate(
                    snapshot.getLocationX(),
                    snapshot.getTargetLocationX(),
                    stepRatio
            ));
            snapshot.setLocationY(interpolate(
                    snapshot.getLocationY(),
                    snapshot.getTargetLocationY(),
                    stepRatio
            ));
            locationTrackingService.save(snapshot);
        } catch (Exception exception) {
            log.warn("Stopping vehicle simulation for externalOrderId={}", externalOrderId, exception);
            stopSimulation(externalOrderId);
        }
    }

    private boolean hasReachedDestination(LocationTrackingSnapshot snapshot) {
        return distanceInKilometers(
                snapshot.getLocationY(),
                snapshot.getLocationX(),
                snapshot.getTargetLocationY(),
                snapshot.getTargetLocationX()
        ) <= properties.getArrivalThresholdKilometers();
    }

    private double interpolate(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    private double distanceInKilometers(
            double startLatitude,
            double startLongitude,
            double targetLatitude,
            double targetLongitude
    ) {
        double earthRadiusKilometers = 6371.0D;
        double latitudeDistance = Math.toRadians(targetLatitude - startLatitude);
        double longitudeDistance = Math.toRadians(targetLongitude - startLongitude);
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(startLatitude))
                * Math.cos(Math.toRadians(targetLatitude))
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);

        return earthRadiusKilometers * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private synchronized void stopSimulation(String externalOrderId) {
        ScheduledFuture<?> future = activeSimulations.remove(externalOrderId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @PreDestroy
    void shutdown() {
        for (String externalOrderId : activeSimulations.keySet()) {
            stopSimulation(externalOrderId);
        }
        scheduler.shutdownNow();
    }
}
