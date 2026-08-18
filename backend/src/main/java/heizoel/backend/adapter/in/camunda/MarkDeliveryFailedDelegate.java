package heizoel.backend.adapter.in.camunda;

import heizoel.backend.application.port.in.workflow.MarkDeliveryFailedUseCase;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("markDeliveryFailedDelegate")
@RequiredArgsConstructor
public class MarkDeliveryFailedDelegate implements JavaDelegate {

    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";

    private final MarkDeliveryFailedUseCase markDeliveryFailedUseCase;

    @Override
    public void execute(DelegateExecution execution) {

        Long confirmationRequestId = ((Number) execution.getVariable(VAR_CONFIRMATION_REQUEST_ID)).longValue();

        markDeliveryFailedUseCase.markDeliveryFailed(confirmationRequestId);
    }

}
