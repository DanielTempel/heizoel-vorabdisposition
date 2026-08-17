package heizoel.backend.configuration;

import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationErrorHandler;
import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationProvider;
import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;


@Configuration
public class SecurityConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiKeyAuthenticationProvider authenticationProvider,
            ApiKeyAuthenticationErrorHandler errorHandler
    ) throws Exception {

        AuthenticationFilter authenticationFilter = getAuthenticationFilter(authenticationProvider, errorHandler);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/dispo/**",
                        "/api/customer/confirmations/**"
                ))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                errorHandler,
                                withDefaults().matcher("/api/dispo/**")
                        )
                        .defaultAuthenticationEntryPointFor(
                                new Http403ForbiddenEntryPoint(),
                                new NegatedRequestMatcher(
                                        withDefaults().matcher("/api/dispo/**")
                                )
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/customer/confirmations/**")
                        .permitAll()

                        .requestMatchers("/api/dispo/**")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
                )

                .addFilterBefore(
                        authenticationFilter,
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }


    private static AuthenticationFilter getAuthenticationFilter(ApiKeyAuthenticationProvider authenticationProvider, ApiKeyAuthenticationErrorHandler errorHandler) {
        ProviderManager authenticationManager = new ProviderManager(authenticationProvider);

        AuthenticationConverter authenticationConverter = request -> {
            String rawApiKey = request.getHeader(API_KEY_HEADER);

            if (rawApiKey == null || rawApiKey.isBlank()) {
                return null;
            }

            return ApiKeyAuthenticationToken.unauthenticated(rawApiKey);
        };

        AuthenticationFilter authenticationFilter =
                new AuthenticationFilter(
                        authenticationManager,
                        authenticationConverter
                );

        authenticationFilter.setRequestMatcher(withDefaults().matcher("/api/dispo/**"));
        authenticationFilter.setFailureHandler(errorHandler);

        authenticationFilter.setSuccessHandler(
                (request, response, authentication) -> {}
        );
        return authenticationFilter;
    }
}
