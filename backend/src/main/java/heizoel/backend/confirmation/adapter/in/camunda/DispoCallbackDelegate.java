package heizoel.backend.confirmation.adapter.in.camunda;

import heizoel.backend.confirmation.adapter.out.dispo.dto.DispoConfirmationStatusUpdateDto;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackService;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("dispoCallbackDelegate")
@RequiredArgsConstructor
public class DispoCallbackDelegate implements JavaDelegate {

    private final DispoStatusCallbackService dispoStatusCallbackService;

    @Override
    public void execute(DelegateExecution execution) {

        String externalOrderId =
                (String) execution.getVariable("externalOrderId");

        ConfirmationStatus confirmationStatus = ConfirmationStatus.valueOf(
                (String) execution.getVariable("confirmationStatus")
        );

        String customerComment =
                (String) execution.getVariable("customerComment");

        DispoConfirmationStatusUpdateDto statusUpdate =
                new DispoConfirmationStatusUpdateDto(
                        externalOrderId,
                        confirmationStatus,
                        customerComment
                );

        dispoStatusCallbackService.sendStatusUpdate(statusUpdate);
    }
}

