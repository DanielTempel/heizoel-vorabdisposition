package heizoel.backend.application.exception;

public class EmailConnectionTestException extends RuntimeException {
  public EmailConnectionTestException(
          String message,
          Throwable cause
  ) {
    super(message, cause);
  }
}