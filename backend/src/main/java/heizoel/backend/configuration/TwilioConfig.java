package heizoel.backend.configuration;

import com.twilio.http.TwilioRestClient;
import heizoel.backend.configuration.properties.TwilioProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    @Bean
    TwilioRestClient twilioRestClient(TwilioProperties properties) {
        return new TwilioRestClient.Builder(
                properties.getAccountSid(),
                properties.getAuthToken()
        ).build();
    }
}
