package heizoel.backend.adapter.out.notification.whatsapp;

import heizoel.backend.adapter.out.notification.NotificationChannelSender;
import heizoel.backend.adapter.out.notification.twilio.TwilioMessageSender;
import heizoel.backend.domain.*;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationSender implements NotificationChannelSender {

    private final ConfirmationProperties properties;
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
        String link = properties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();

        twilioMessageSender.sendWhatsApp(
                order,
                confirmationRequest,
                "Bitte bestaetigen Sie Ihren Liefertermin: " + link
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
