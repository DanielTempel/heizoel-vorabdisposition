package heizoel.backend.adapter.out.notification.sms;

import heizoel.backend.adapter.out.notification.NotificationChannelSender;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.domain.*;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationSender implements NotificationChannelSender {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

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

        SmsSendRequestDto request = new SmsSendRequestDto(
                order.getCustomerPhoneNumber(),
                "Lieferung bestaetigen: " + link
        );

        try {
            restClient.post()
                    .uri(properties.getSmsProviderUrl())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new NotificationDeliveryException(
                    CommunicationChannel.SMS,
                    "Notification could not be delivered for externalOrderId="
                            + order.getExternalOrderId(),
                    ex
            );
        }
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
