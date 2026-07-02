package heizoel.backend.confirmation.infrastructure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.confirmation")
public class ConfirmationProperties {

    private String frontendUrl = "http://localhost:3000";
    private String dispoUrl;
    private String dispoTrackingUrl;
    private String smsProviderUrl;
    private String whatsappProviderUrl;
}

