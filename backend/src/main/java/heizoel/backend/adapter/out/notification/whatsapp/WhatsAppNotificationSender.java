package heizoel.backend.adapter.out.notification.whatsapp;

import heizoel.backend.adapter.out.notification.ConfirmationMessageContent;
import heizoel.backend.adapter.out.notification.NotificationChannelSender;
import heizoel.backend.adapter.out.notification.twilio.TwilioMessageSender;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationSender implements NotificationChannelSender {

    private final ConfirmationProperties confirmationProperties;
    private final TwilioMessageSender twilioMessageSender;

    @Override
    public CommunicationChannel channel() {
        return CommunicationChannel.WHATSAPP;
    }

    @Override
    public void sendConfirmationRequest(
            Order order,
            ConfirmationRequest confirmationRequest
    ) {
        twilioMessageSender.sendWhatsApp(
                order.getExternalOrderId(),
                order.getCustomerPhoneNumber(),
                ConfirmationMessageContent.from(
                        order,
                        confirmationRequest,
                        confirmationProperties.getFrontendUrl()
                )
        );
    }

    @Override
    public void sendCustomerResponseReceived(
            Order order,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        log.info(
                "Customer response follow-up WhatsApp message skipped because it is not implemented in the MVP. externalOrderId={}, responseType={}",
                order.getExternalOrderId(),
                responseType
        );
    }
}
