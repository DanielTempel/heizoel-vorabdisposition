package heizoel.backend.adapter.in.camunda;


import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import heizoel.backend.domain.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("noResponseTimeoutDelegate")
@RequiredArgsConstructor
public class NoResponseTimeoutDelegate implements JavaDelegate {

    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";
    private static final String VAR_ORDER_ID = "orderId";
    private static final String VAR_CONFIRMATION_STATUS = "confirmationStatus";
    private static final String VAR_CUSTOMER_COMMENT = "customerComment";

    private final HandleNoResponseTimeoutUseCase handleNoResponseTimeoutUseCase;

    @Override
    public void execute(DelegateExecution execution) {

        Long confirmationRequestId = ((Number) execution.getVariable(VAR_CONFIRMATION_REQUEST_ID)).longValue();
        Long orderId = handleNoResponseTimeoutUseCase.handleTimeout(confirmationRequestId);
        execution.setVariable(VAR_ORDER_ID, orderId);
        execution.setVariable(VAR_CONFIRMATION_STATUS, ConfirmationStatus.NO_RESPONSE.name());
        execution.setVariable(VAR_CUSTOMER_COMMENT, null);
    }
}

