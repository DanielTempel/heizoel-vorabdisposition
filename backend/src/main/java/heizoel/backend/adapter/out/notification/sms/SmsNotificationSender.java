package heizoel.backend.adapter.out.notification.sms;

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
public class SmsNotificationSender implements NotificationChannelSender {

    private final ConfirmationProperties properties;
    private final TwilioMessageSender twilioMessageSender;

    @Override
    public CommunicationChannel channel() {
        return CommunicationChannel.SMS;
    }

    @Override
    public void sendConfirmationRequest(
            Order order,
            ConfirmationRequest confirmationRequest
    ) {
        String link = properties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();

        twilioMessageSender.sendSms(
                order,
                confirmationRequest,
                "Lieferung bestaetigen: " + link
        );
    }

    @Override
    public void sendCustomerResponseReceived(
            Order order,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        log.info(
                "Customer response follow-up SMS skipped because it is not implemented in the MVP. externalOrderId={}, responseType={}",
                order.getExternalOrderId(),
                responseType
        );
    }
}
