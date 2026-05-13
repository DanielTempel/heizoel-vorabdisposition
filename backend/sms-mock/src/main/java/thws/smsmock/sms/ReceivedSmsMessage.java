package thws.smsmock.sms;

import java.time.Instant;

public record ReceivedSmsMessage(
        Instant receivedAt,
        String to,
        String text
) {
}