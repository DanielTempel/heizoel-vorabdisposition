package heizoel.backend.dispo.infrastructure;

import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;
import heizoel.backend.dispo.application.interfaces.DispoStatusCallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockDispoStatusCallbackService implements DispoStatusCallbackService {

    @Override
    public void sendStatusUpdate(DispoConfirmationStatusUpdateDto statusUpdate) {
        log.info(
                "Mock DISPO callback sent. externalOrderId={}, confirmationStatus={}, customerComment={}",
                statusUpdate.externalOrderId(),
                statusUpdate.confirmationStatus(),
                statusUpdate.customerComment()
        );
    }
}