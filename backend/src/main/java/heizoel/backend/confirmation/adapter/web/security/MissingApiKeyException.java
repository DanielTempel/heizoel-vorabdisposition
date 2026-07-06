package heizoel.backend.confirmation.adapter.web.security;

public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException(String message) {
        super(message);
    }
}
