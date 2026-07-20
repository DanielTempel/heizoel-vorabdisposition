package heizoel.backend.adapter.in.camunda;


import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("noResponseTimeoutDelegate")
@RequiredArgsConstructor
public class NoResponseTimeoutDelegate implements JavaDelegate {

    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";
    private final HandleNoResponseTimeoutUseCase handleNoResponseTimeoutUseCase;

    @Override
    public void execute(DelegateExecution execution) {
        Long confirmationRequestId = (Long) execution.getVariable(VAR_CONFIRMATION_REQUEST_ID);
        handleNoResponseTimeoutUseCase.handleTimeout(confirmationRequestId);
    }

}

