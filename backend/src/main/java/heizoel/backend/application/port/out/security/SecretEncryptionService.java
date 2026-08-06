package heizoel.backend.application.port.out.security;

public interface SecretEncryptionService {

    String encrypt(String plaintext, String context);

    String decrypt(String encryptedValue, String context);
}


