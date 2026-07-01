package heizoel.backend.confirmation.adapter.out.dispo;

import heizoel.backend.confirmation.adapter.out.dispo.dto.DispoConfirmationStatusUpdateDto;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("mock-dispo-callback")
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
