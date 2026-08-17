package heizoel.backend.application.exception;

public class InvalidEmailSettingsException extends RuntimeException {
    public InvalidEmailSettingsException(String message) {
        super(message);
    }
}
