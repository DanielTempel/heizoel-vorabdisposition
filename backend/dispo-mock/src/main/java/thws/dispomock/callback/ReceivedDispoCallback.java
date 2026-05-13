package thws.dispomock.callback;

import java.time.Instant;

public record ReceivedDispoCallback(
        Instant receivedAt,
        String externalOrderId,
        String confirmationStatus,
        String customerComment
) {
}