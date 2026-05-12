package heizoel.backend.dispo.application;


import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;
import heizoel.backend.dispo.application.interfaces.DispoStatusCallbackService;
import heizoel.backend.dispo.application.model.command.CustomerConfirmationStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispoStatusCallbackListener {

    private final DispoStatusCallbackService dispoStatusCallbackService;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerConfirmationStatusChanged(CustomerConfirmationStatusChangedEvent event) {
        try {
            DispoConfirmationStatusUpdateDto statusUpdate =
                    new DispoConfirmationStatusUpdateDto(
                            event.externalOrderId(),
                            event.confirmationStatus(),
                            event.customerComment()
                    );

            dispoStatusCallbackService.sendStatusUpdate(statusUpdate);
        } catch (Exception ex) {
            log.error(
                    "Failed to send DISPO status callback for externalOrderId={}",
                    event.externalOrderId(),
                    ex
            );
        }
    }
}
