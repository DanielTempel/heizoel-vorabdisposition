package heizoel.backend.notification.application.sms;


import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.exceptions.notification.SmsSendingException;
import heizoel.backend.notification.application.interfaces.SmsConfirmationSender;
import heizoel.backend.notification.application.sms.web.SmsSendRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class SmsConfirmationSenderImpl implements SmsConfirmationSender {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

    @Override
    public void send(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        String link = properties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();

        String text = "Lieferung bestaetigen: " + link;

        SmsSendRequestDto request = new SmsSendRequestDto(
                orderSnapshot.getCustomerPhoneNumber(),
                text
        );

        try {
            restClient.post()
                    .uri(properties.getSmsProviderUrl())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new SmsSendingException(
                    "SMS could not be sent for externalOrderId=" + orderSnapshot.getExternalOrderId()
            );
        }
    }
}