package heizoel.backend.adapter.in.web.security;

public interface ApiKeyHasher {
    String hash(String rawApiKey);
}