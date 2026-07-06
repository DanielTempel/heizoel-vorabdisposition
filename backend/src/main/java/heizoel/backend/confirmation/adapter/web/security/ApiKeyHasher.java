package heizoel.backend.confirmation.adapter.web.security;

public interface ApiKeyHasher {
    String hash(String rawApiKey);
}