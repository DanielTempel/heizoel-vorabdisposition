package heizoel.backend.location.application.interfaces;

import heizoel.backend.location.domain.VehicleSimulationStartResult;

public interface VehicleSimulationService {

    VehicleSimulationStartResult startSimulation(String externalOrderId);
}
