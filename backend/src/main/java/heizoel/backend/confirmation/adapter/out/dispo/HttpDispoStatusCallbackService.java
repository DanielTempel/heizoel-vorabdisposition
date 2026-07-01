package heizoel.backend.confirmation.adapter.out.dispo;

import heizoel.backend.confirmation.adapter.out.dispo.dto.DispoConfirmationStatusUpdateDto;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackRequest;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackService;
import heizoel.backend.confirmation.infrastructure.properties.ConfirmationProperties;
import heizoel.backend.shared.exception.DispoCallbackFailedException;
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
    public void sendStatusUpdate(DispoStatusCallbackRequest request) {
        DispoConfirmationStatusUpdateDto dto = new DispoConfirmationStatusUpdateDto(
                request.externalOrderId(),
                request.confirmationStatus(),
                request.customerComment()
        );

        try {
            restClient.post()
                    .uri(properties.getDispoUrl())
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new DispoCallbackFailedException(
                    "DISPO callback failed for externalOrderId="
                            + request.externalOrderId()
                            + ", status="
                            + request.confirmationStatus()
            );
        }
    }
}