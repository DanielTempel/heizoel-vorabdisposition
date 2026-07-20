package heizoel.backend.adapter.in.web.security;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
