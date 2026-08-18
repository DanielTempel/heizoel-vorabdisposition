package heizoel.backend.adapter.out.security;

import heizoel.backend.application.exception.SecretEncryptionException;
import heizoel.backend.application.port.out.security.SecretEncryptionService;
import heizoel.backend.configuration.properties.SecretEncryptionProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmSecretEncryptionService implements SecretEncryptionService {


    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String FORMAT_VERSION = "v1";

    private static final int AES_256_KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AUTHENTICATION_TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretEncryptionService(
            SecretEncryptionProperties properties
    ) {
        this.secretKey = readMasterKey(properties.getMasterKey());
    }

    @Override
    public String encrypt(
            String plaintext,
            String context
    ) {

        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_LENGTH_BITS,
                            iv
                    )
            );

            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return FORMAT_VERSION
                    + ":"
                    + encode(iv)
                    + ":"
                    + encode(ciphertext);

        } catch (GeneralSecurityException exception) {
            throw new SecretEncryptionException(
                    "Secret could not be encrypted.",
                    exception
            );
        }
    }

    @Override
    public String decrypt(
            String encryptedValue,
            String context
    ) {

        String[] parts = encryptedValue.split(":", -1);

        if (parts.length != 3) {
            throw new SecretEncryptionException(
                    "Encrypted secret has an invalid format."
            );
        }

        if (!FORMAT_VERSION.equals(parts[0])) {
            throw new SecretEncryptionException(
                    "Encrypted secret version is not supported."
            );
        }

        try {
            byte[] iv = decode(parts[1]);
            byte[] ciphertext = decode(parts[2]);

            if (iv.length != IV_LENGTH_BYTES) {
                throw new SecretEncryptionException(
                        "Encrypted secret contains an invalid IV."
                );
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_LENGTH_BITS,
                            iv
                    )
            );

            cipher.updateAAD(
                    context.getBytes(StandardCharsets.UTF_8)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new SecretEncryptionException(
                    "Secret could not be decrypted.",
                    exception
            );
        }
    }

    private SecretKey readMasterKey(String configuredMasterKey) {
        if (configuredMasterKey == null || configuredMasterKey.isBlank()) {
            throw new IllegalStateException(
                    "Secret encryption master key is not configured."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredMasterKey);

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Secret encryption master key must be Base64 encoded.",
                    exception
            );
        }

        if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "Secret encryption master key must contain exactly "
                            + AES_256_KEY_LENGTH_BYTES
                            + " bytes after Base64 decoding."
            );
        }

        return new SecretKeySpec(
                keyBytes,
                KEY_ALGORITHM
        );
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}