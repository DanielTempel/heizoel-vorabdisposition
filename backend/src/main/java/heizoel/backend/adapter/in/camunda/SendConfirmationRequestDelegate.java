package heizoel.backend.adapter.in.camunda;


import heizoel.backend.application.port.in.workflow.SendConfirmationRequestResult;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestUseCase;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("sendConfirmationRequestDelegate")
@RequiredArgsConstructor
public class SendConfirmationRequestDelegate implements JavaDelegate {

    private static final String ERROR_DELIVERY_FAILED = "NOTIFICATION_DELIVERY_FAILED";
    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";
    private static final String VAR_DELIVERY_ATTEMPT = "deliveryAttempt";
    private static final String VAR_MAX_DELIVERY_ATTEMPTS = "maxDeliveryAttempts";
    private static final String VAR_RETRY_DELAY = "retryDelay";
    private static final String VAR_RESPONSE_DEADLINE_AT = "responseDeadlineAt";

    private static final String RETRY_DELAY_AFTER_FIRST_ATTEMPT = "PT1M";
    private static final String RETRY_DELAY_AFTER_SECOND_ATTEMPT = "PT5M";

    private final SendConfirmationRequestUseCase sendConfirmationRequestUseCase;


    @Override
    public void execute(DelegateExecution execution) {

        Long confirmationRequestId = ((Number) execution.getVariable(VAR_CONFIRMATION_REQUEST_ID)).longValue();
        int attempt = ((Number) execution.getVariable(VAR_DELIVERY_ATTEMPT)).intValue() + 1;
        int maxAttempts = ((Number) execution.getVariable(VAR_MAX_DELIVERY_ATTEMPTS)).intValue();

        execution.setVariable(
                VAR_DELIVERY_ATTEMPT,
                attempt
        );

        SendConfirmationRequestResult result =
                sendConfirmationRequestUseCase.send(
                        confirmationRequestId
                );

        switch (result.outcome()) {

            case SENT -> execution.setVariable(
                    VAR_RESPONSE_DEADLINE_AT,
                    result.responseDeadlineAt().toString()
            );


            case RETRYABLE_FAILURE -> {
                if (attempt < maxAttempts) {
                    execution.setVariable(
                            VAR_RETRY_DELAY,
                            retryDelayAfter(attempt)
                    );
                }

                throw new BpmnError(
                        ERROR_DELIVERY_FAILED
                );
            }


            case PERMANENT_FAILURE -> {
                /*
                 * Force the BPMN gateway directly onto FAILED.
                 */
                execution.setVariable(
                        VAR_DELIVERY_ATTEMPT,
                        maxAttempts
                );

                throw new BpmnError(
                        ERROR_DELIVERY_FAILED
                );
            }
        }
    }

        private String retryDelayAfter(int attempt) {
            return switch (attempt) {
                case 1 -> RETRY_DELAY_AFTER_FIRST_ATTEMPT;
                case 2 -> RETRY_DELAY_AFTER_SECOND_ATTEMPT;
                default -> throw new IllegalStateException(
                        "No retry delay configured after attempt "
                                + attempt
                );
            };
        }
}
