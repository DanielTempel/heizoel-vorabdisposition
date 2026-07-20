package heizoel.backend.adapter.in.web.security;

public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException(String message) {
        super(message);
    }
}
