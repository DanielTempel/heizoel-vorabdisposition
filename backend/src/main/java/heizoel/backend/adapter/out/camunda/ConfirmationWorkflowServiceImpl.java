package heizoel.backend.adapter.out.camunda;

import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class ConfirmationWorkflowServiceImpl implements ConfirmationWorkflowService {

    private static final String PROCESS_KEY = "confirmation-request-process";
    private static final String VAR_DELIVERY_ATTEMPT = "deliveryAttempt";
    private static final String VAR_MAX_DELIVERY_ATTEMPTS = "maxDeliveryAttempts";
    private static final String VAR_ORDER_ID = "orderId";
    private static final String VAR_CONFIRMATION_STATUS = "confirmationStatus";
    private static final String VAR_CUSTOMER_COMMENT = "customerComment";
    private static final String MESSAGE_CUSTOMER_RESPONSE_RECEIVED = "CustomerResponseReceived";
    private static final String MESSAGE_CONFIRMATION_REQUEST_SUPERSEDED = "ConfirmationRequestSuperseded";

    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    private final RuntimeService runtimeService;

    @Override
    public void startDeliveryProcess(Long confirmationRequestId) {
        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                confirmationRequestId.toString(),
                Map.of(
                        VAR_DELIVERY_ATTEMPT,
                        0,
                        VAR_MAX_DELIVERY_ATTEMPTS,
                        MAX_DELIVERY_ATTEMPTS
                )
        );
    }

    @Override
    public void notifyCustomerResponseReceived(
            Long confirmationRequestId,
            Long orderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    ) {
        Map<String, Object> variables = new HashMap<>();

        variables.put(VAR_ORDER_ID, orderId);
        variables.put(VAR_CONFIRMATION_STATUS, confirmationStatus.name());
        variables.put(VAR_CUSTOMER_COMMENT, customerComment);

        runtimeService
                .createMessageCorrelation(MESSAGE_CUSTOMER_RESPONSE_RECEIVED)
                .processInstanceBusinessKey(confirmationRequestId.toString())
                .setVariables(variables)
                .correlate();
    }

    @Override
    public void notifyConfirmationRequestSuperseded(
            Long confirmationRequestId
    ) {
        runtimeService
                .createMessageCorrelation(MESSAGE_CONFIRMATION_REQUEST_SUPERSEDED)
                .processInstanceBusinessKey(confirmationRequestId.toString())
                .correlate();
    }

}
