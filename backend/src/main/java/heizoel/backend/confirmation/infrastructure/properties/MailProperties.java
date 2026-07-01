package heizoel.backend.confirmation.infrastructure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "heizoel.mail")
public class MailProperties {

    private String from = "no-reply@heizoel.local";
}
