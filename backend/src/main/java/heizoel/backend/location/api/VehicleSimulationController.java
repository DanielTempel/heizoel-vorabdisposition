package heizoel.backend.location.api;

import heizoel.backend.location.api.dto.VehicleSimulationStartResponseDto;
import heizoel.backend.location.application.interfaces.VehicleSimulationService;
import heizoel.backend.location.domain.VehicleSimulationStartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispo/confirmation-requests")
@RequiredArgsConstructor
public class VehicleSimulationController {

    private final VehicleSimulationService vehicleSimulationService;

    @PostMapping("/{externalOrderId}/vehicle-simulation/start")
    public ResponseEntity<VehicleSimulationStartResponseDto> startVehicleSimulation(
            @PathVariable String externalOrderId
    ) {
        VehicleSimulationStartResult result = vehicleSimulationService.startSimulation(externalOrderId);
        return ResponseEntity.accepted().body(new VehicleSimulationStartResponseDto(
                result.externalOrderId(),
                result.simulationStatus()
        ));
    }
}
