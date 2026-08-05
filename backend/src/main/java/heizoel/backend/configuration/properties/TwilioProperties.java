package heizoel.backend.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.twilio")
public class TwilioProperties {

    private String accountSid;
    private String authToken;
    private String smsFrom;
    private String whatsappFrom;
    private String contentTemplateSid;
}
