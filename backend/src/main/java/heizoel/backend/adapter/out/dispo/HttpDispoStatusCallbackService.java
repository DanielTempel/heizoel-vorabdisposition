package heizoel.backend.adapter.out.dispo;

import heizoel.backend.application.port.out.dispo.DispoCallbackException;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class HttpDispoStatusCallbackService implements DispoStatusCallbackService {

    private final RestClient restClient;

    @Override
    public void sendStatusUpdate(DispoStatusCallbackRequest request) {
        DispoConfirmationStatusUpdateDto dto = new DispoConfirmationStatusUpdateDto(
                request.externalOrderId(),
                request.confirmationStatus(),
                request.customerComment()
        );

        try {
            restClient.post()
                    .uri(request.callbackUrl())
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new DispoCallbackException(
                    "DISPO callback failed for externalOrderId="
                            + request.externalOrderId()
                            + ", status="
                            + request.confirmationStatus(),
                    ex
            );
        }
    }
}
