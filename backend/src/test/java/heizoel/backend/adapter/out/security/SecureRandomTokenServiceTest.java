package heizoel.backend.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomTokenServiceTest {

    private final SecureRandomTokenService tokenService =
            new SecureRandomTokenService();

    @Test
    void generatesUrlSafeTokenWithExpectedEntropyLength() {
        String token = tokenService.generateToken();

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(token).doesNotContain("=");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }

    @Test
    void generatesDifferentTokens() {
        assertThat(tokenService.generateToken())
                .isNotEqualTo(tokenService.generateToken());
    }
}
