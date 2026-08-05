package heizoel.backend.adapter.out.notification.whatsapp;

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
public class WhatsAppNotificationSender implements NotificationChannelSender {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

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

        WhatsAppSendRequestDto request = new WhatsAppSendRequestDto(
                order.getCustomerPhoneNumber(),
                "Bitte bestätigen Sie Ihren Liefertermin: " + link
        );

        try {
            restClient.post()
                    .uri(properties.getWhatsappProviderUrl())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new NotificationDeliveryException(
                    CommunicationChannel.WHATSAPP,
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
                "Customer response follow-up WhatsApp message skipped because it is not implemented in the MVP. externalOrderId={}, responseType={}",
                order.getExternalOrderId(),
                responseType
        );
    }
}
