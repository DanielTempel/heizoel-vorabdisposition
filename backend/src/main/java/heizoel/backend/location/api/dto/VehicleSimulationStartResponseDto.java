package heizoel.backend.location.api.dto;

import heizoel.backend.location.domain.VehicleSimulationStatus;

public record VehicleSimulationStartResponseDto(
        String externalOrderId,
        VehicleSimulationStatus simulationStatus
) {
}
