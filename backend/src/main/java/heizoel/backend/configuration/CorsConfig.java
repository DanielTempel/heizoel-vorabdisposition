package heizoel.backend.configuration;

import heizoel.backend.configuration.properties.ConfirmationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final ConfirmationProperties confirmationProperties;

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {

                registry.addMapping("/api/dashboard/**")
                        .allowedOrigins(confirmationProperties.getFrontendUrl())
                        .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                        .allowedHeaders(
                                "Content-Type",
                                "X-CSRF-TOKEN"
                        )
                        .allowCredentials(true);

                registry.addMapping("/api/customer/confirmations/**")
                        .allowedOrigins(confirmationProperties.getFrontendUrl())
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("Content-Type");
            }
        };
    }
}

