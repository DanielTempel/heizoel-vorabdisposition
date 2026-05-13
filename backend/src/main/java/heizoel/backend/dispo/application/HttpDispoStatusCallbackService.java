package heizoel.backend.dispo.application;


import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;
import heizoel.backend.dispo.application.interfaces.DispoStatusCallbackService;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.exceptions.dispo.DispoCallbackFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class HttpDispoStatusCallbackService implements DispoStatusCallbackService {

    private final RestClient restClient;
    private final ConfirmationProperties properties;

    @Override
    public void sendStatusUpdate(DispoConfirmationStatusUpdateDto statusUpdate) {
        try {
            restClient.post()
                    .uri(properties.getDispoUrl())
                    .body(statusUpdate)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new DispoCallbackFailedException(
                    "DISPO callback failed for externalOrderId="
                            + statusUpdate.externalOrderId()
                            + ", status="
                            + statusUpdate.confirmationStatus()
            );
        }
    }


}
