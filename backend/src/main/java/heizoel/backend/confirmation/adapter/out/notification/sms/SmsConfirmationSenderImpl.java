package heizoel.backend.confirmation.adapter.out.notification.sms;


import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.infrastructure.properties.ConfirmationProperties;
import heizoel.backend.shared.exception.SmsSendingException;
import heizoel.backend.confirmation.application.port.out.SmsConfirmationSender;
import heizoel.backend.confirmation.adapter.out.notification.sms.SmsSendRequestDto;
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
