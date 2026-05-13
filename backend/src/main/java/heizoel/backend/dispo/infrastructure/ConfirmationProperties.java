package heizoel.backend.dispo.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.confirmation")
public class ConfirmationProperties {

    private Duration responseDeadline = Duration.ofHours(24);
    private String frontendUrl = "http://localhost:3000";
    private String dispoUrl;
}