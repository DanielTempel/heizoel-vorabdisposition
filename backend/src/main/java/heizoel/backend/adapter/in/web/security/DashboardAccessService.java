package heizoel.backend.adapter.in.web.security;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class DashboardAccessService {

    private static final Duration CODE_TTL = Duration.ofMinutes(1);
    private static final int CODE_LENGTH_BYTES = 32;

    private final Clock clock;
    private final ConfirmationProperties confirmationProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentMap<String, DashboardAccess> accesses =
            new ConcurrentHashMap<>();


    public String createRedirectUrl(Long companyId) {
        Instant now = clock.instant();

        removeExpired(now);

        String code = generateCode();

        accesses.put(
                code,
                new DashboardAccess(
                        companyId,
                        now.plus(CODE_TTL)
                )
        );

        return UriComponentsBuilder
                .fromUriString(confirmationProperties.getFrontendUrl())
                .path("/login")
                .queryParam("code", code)
                .build()
                .toUriString();
    }


    public CompanyContext consume(String code) {
        DashboardAccess access = accesses.remove(code);

        if (access == null) {
            throw new BadCredentialsException(
                    "Invalid dashboard access code."
            );
        }

        if (!access.expiresAt().isAfter(clock.instant())) {
            throw new BadCredentialsException(
                    "Expired dashboard access code."
            );
        }

        return new CompanyContext(access.companyId());
    }


    private String generateCode() {
        byte[] bytes = new byte[CODE_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }


    private void removeExpired(Instant now) {
        accesses.entrySet().removeIf(
                entry -> !entry.getValue().expiresAt().isAfter(now)
        );
    }


    private record DashboardAccess(
            Long companyId,
            Instant expiresAt
    ) {
    }
}