package heizoel.backend.location.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.location.simulation")
public class VehicleSimulationProperties {

    private long tickIntervalMillis = 1000L;
    private double arrivalThresholdKilometers = 0.08D;
    private double minimumStepKilometers = 0.18D;
    private double maximumStepKilometers = 0.65D;
    private double maximumStepRatio = 0.18D;
}
