package heizoel.backend.configuration.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(
        prefix = "heizoel.security.secret-encryption"
)
public class SecretEncryptionProperties {

    private String masterKey;
}
