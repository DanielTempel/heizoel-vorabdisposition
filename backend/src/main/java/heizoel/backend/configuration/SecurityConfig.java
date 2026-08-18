package heizoel.backend.configuration;

import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationErrorHandler;
import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationProvider;
import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;


@Configuration
public class SecurityConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    SecurityContextRepository dashboardSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    SecurityFilterChain dispoSecurityFilterChain(
            HttpSecurity http,
            ApiKeyAuthenticationProvider authenticationProvider,
            ApiKeyAuthenticationErrorHandler errorHandler
    ) throws Exception {

        AuthenticationFilter authenticationFilter =
                getAuthenticationFilter(authenticationProvider, errorHandler);

        http
                .securityMatcher("/api/dispo/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        authenticationFilter,
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository dashboardSecurityContextRepository
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/customer/confirmations/**",
                                "/api/dashboard/auth/exchange"
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(
                                dashboardSecurityContextRepository
                        )
                )
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                withDefaults().matcher("/api/dashboard/**")
                        )
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/customer/confirmations/**")
                        .permitAll()

                        .requestMatchers("/api/dashboard/auth/exchange")
                        .permitAll()

                        .requestMatchers("/api/dashboard/**")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
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
