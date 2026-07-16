package heizoel.backend.confirmation.adapter.camunda;

import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackCommand;
import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackUseCase;
import heizoel.backend.domain.model.enumeration.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("dispoCallbackDelegate")
@RequiredArgsConstructor
public class DispoCallbackDelegate implements JavaDelegate {

    private static final String VAR_ORDER_SNAPSHOT_ID = "orderSnapshotId";
    private static final String VAR_CONFIRMATION_STATUS = "confirmationStatus";
    private static final String VAR_CUSTOMER_COMMENT = "customerComment";

    private final SendDispoStatusCallbackUseCase sendDispoStatusCallbackUseCase;

    @Override
    public void execute(DelegateExecution execution) {
        SendDispoStatusCallbackCommand command = new SendDispoStatusCallbackCommand(
                (Long) execution.getVariable(VAR_ORDER_SNAPSHOT_ID),
                ConfirmationStatus.valueOf((String) execution.getVariable(VAR_CONFIRMATION_STATUS)),
                (String) execution.getVariable(VAR_CUSTOMER_COMMENT)
        );

        sendDispoStatusCallbackUseCase.sendDispoStatusCallback(command);
    }
}
