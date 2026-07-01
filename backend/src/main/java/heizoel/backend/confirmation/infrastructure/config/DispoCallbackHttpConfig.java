package heizoel.backend.confirmation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DispoCallbackHttpConfig {

    @Bean
    RestClient dispoRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}

