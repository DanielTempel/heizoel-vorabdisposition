package heizoel.backend.dispo.infrastructure.error;

import java.time.Instant;

public record ErrorResponseDto(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp) {
}
