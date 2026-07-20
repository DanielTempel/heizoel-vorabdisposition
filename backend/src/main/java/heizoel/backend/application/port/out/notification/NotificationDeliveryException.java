package heizoel.backend.application.port.out.notification;

import heizoel.backend.domain.CommunicationChannel;

public class NotificationDeliveryException extends RuntimeException {

    private final CommunicationChannel channel;

    public NotificationDeliveryException(
            CommunicationChannel channel,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.channel = channel;
    }

    public CommunicationChannel getChannel() {
        return channel;
    }
}
