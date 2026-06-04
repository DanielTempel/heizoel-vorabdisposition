package heizoel.backend.location.domain;

public record VehicleSimulationStartResult(
        String externalOrderId,
        VehicleSimulationStatus simulationStatus
) {
}
