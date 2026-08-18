package heizoel.backend.adapter.out.security;

import heizoel.backend.application.exception.SecretEncryptionException;
import heizoel.backend.configuration.properties.SecretEncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSecretEncryptionServiceTest {

    private static final String CONTEXT =
            "company-email-settings:1";

    private AesGcmSecretEncryptionService service;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);

        SecretEncryptionProperties properties =
                new SecretEncryptionProperties();

        properties.setMasterKey(
                Base64.getEncoder().encodeToString(key)
        );

        service = new AesGcmSecretEncryptionService(properties);
    }

    @Test
    void encryptsAndDecryptsSecret() {
        String encrypted = service.encrypt(
                "smtp-password",
                CONTEXT
        );

        String decrypted = service.decrypt(
                encrypted,
                CONTEXT
        );

        assertThat(decrypted).isEqualTo("smtp-password");
        assertThat(encrypted).doesNotContain("smtp-password");
    }

    @Test
    void usesDifferentIvForEveryEncryption() {
        String first = service.encrypt(
                "same-password",
                CONTEXT
        );

        String second = service.encrypt(
                "same-password",
                CONTEXT
        );

        assertThat(first).isNotEqualTo(second);

        assertThat(service.decrypt(first, CONTEXT))
                .isEqualTo("same-password");

        assertThat(service.decrypt(second, CONTEXT))
                .isEqualTo("same-password");
    }

    @Test
    void rejectsDifferentCompanyContext() {
        String encrypted = service.encrypt(
                "smtp-password",
                "company-email-settings:1"
        );

        assertThatThrownBy(() ->
                service.decrypt(
                        encrypted,
                        "company-email-settings:2"
                )
        )
                .isInstanceOf(SecretEncryptionException.class)
                .hasMessage("Secret could not be decrypted.");
    }

    @Test
    void rejectsMalformedEncryptedValue() {
        assertThatThrownBy(() ->
                service.decrypt(
                        "not-an-encrypted-secret",
                        CONTEXT
                )
        )
                .isInstanceOf(SecretEncryptionException.class)
                .hasMessage(
                        "Encrypted secret has an invalid format."
                );
    }

    @Test
    void rejectsInvalidMasterKeyLength() {
        SecretEncryptionProperties properties =
                new SecretEncryptionProperties();

        properties.setMasterKey(
                Base64.getEncoder()
                        .encodeToString(new byte[16])
        );

        assertThatThrownBy(() ->
                new AesGcmSecretEncryptionService(properties)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 32 bytes");
    }
}