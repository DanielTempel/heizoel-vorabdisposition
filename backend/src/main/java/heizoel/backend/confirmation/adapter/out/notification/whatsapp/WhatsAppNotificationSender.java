package heizoel.backend.confirmation.adapter.out.notification.whatsapp;

import heizoel.backend.confirmation.adapter.out.notification.NotificationChannelSender;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.confirmation.infrastructure.properties.ConfirmationProperties;
import heizoel.backend.shared.exception.WhatsAppSendingException;
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
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        String link = properties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();

        WhatsAppSendRequestDto request = new WhatsAppSendRequestDto(
                orderSnapshot.getCustomerPhoneNumber(),
                "Bitte bestätigen Sie Ihren Liefertermin: " + link
        );

        try {
            restClient.post()
                    .uri(properties.getWhatsappProviderUrl())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new WhatsAppSendingException(
                    "WhatsApp message could not be sent for externalOrderId="
                            + orderSnapshot.getExternalOrderId(),
                    ex
            );
        }
    }

    @Override
    public void sendCustomerResponseReceived(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        log.info(
                "Customer response follow-up WhatsApp message skipped because it is not implemented in the MVP. externalOrderId={}, responseType={}",
                orderSnapshot.getExternalOrderId(),
                responseType
        );
    }
}
