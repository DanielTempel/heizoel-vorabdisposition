package heizoel.backend.application.exception;

public class SecretEncryptionException extends RuntimeException {

  public SecretEncryptionException(
          String message,
          Throwable cause
  ) {
    super(message, cause);
  }

  public SecretEncryptionException(String message) {
    super(message);
  }
}