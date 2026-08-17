package heizoel.backend.application.exception;

public class EmailSettingsNotConfiguredException extends RuntimeException {
    public EmailSettingsNotConfiguredException(String message) {
        super(message);
    }
}
