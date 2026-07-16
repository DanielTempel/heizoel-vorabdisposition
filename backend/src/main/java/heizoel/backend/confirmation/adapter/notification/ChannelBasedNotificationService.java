package heizoel.backend.confirmation.adapter.notification;

import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.application.port.out.notification.NotificationService;
import heizoel.backend.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.domain.model.enumeration.CustomerResponseType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ChannelBasedNotificationService implements NotificationService {

    private final Map<CommunicationChannel, NotificationChannelSender> sendersByChannel;

    public ChannelBasedNotificationService(List<NotificationChannelSender> senders) {
        this.sendersByChannel = buildSenderMap(senders);
    }

    @Override
    public void sendConfirmationRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        senderFor(confirmationRequest.getCommunicationChannel())
                .sendConfirmationRequest(orderSnapshot, confirmationRequest);
    }

    @Override
    public void sendCustomerResponseReceived(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        senderFor(confirmationRequest.getCommunicationChannel())
                .sendCustomerResponseReceived(
                        orderSnapshot,
                        confirmationRequest,
                        responseType
                );
    }

    private Map<CommunicationChannel, NotificationChannelSender> buildSenderMap(
            List<NotificationChannelSender> senders
    ) {
        Map<CommunicationChannel, NotificationChannelSender> map =
                new EnumMap<>(CommunicationChannel.class);

        for (NotificationChannelSender sender : senders) {
            NotificationChannelSender duplicate = map.put(sender.channel(), sender);
            if (duplicate != null) {
                throw new UnsupportedCommunicationChannelException(
                        "Multiple notification senders are registered for channel "
                                + sender.channel()
                                + ": "
                                + duplicate.getClass().getSimpleName()
                                + " and "
                                + sender.getClass().getSimpleName()
                );
            }
        }

        return Map.copyOf(map);
    }

    private NotificationChannelSender senderFor(CommunicationChannel channel) {
        NotificationChannelSender sender = sendersByChannel.get(channel);

        if (sender == null) {
            throw new UnsupportedCommunicationChannelException(
                    "No notification sender is registered for channel " + channel
            );
        }

        return sender;
    }
}
