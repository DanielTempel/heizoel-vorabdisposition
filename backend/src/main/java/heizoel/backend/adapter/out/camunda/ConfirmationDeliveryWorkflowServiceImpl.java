package heizoel.backend.adapter.out.camunda;

import heizoel.backend.application.port.out.workflow.ConfirmationDeliveryWorkflowService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class ConfirmationDeliveryWorkflowServiceImpl implements ConfirmationDeliveryWorkflowService {

    private static final String PROCESS_KEY = "confirmation-delivery-process";

    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";

    private final RuntimeService runtimeService;

    @Override
    public void startDeliveryProcess(Long confirmationRequestId) {
        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                confirmationRequestId.toString(),
                Map.of(
                        VAR_CONFIRMATION_REQUEST_ID,
                        confirmationRequestId
                )
        );
    }
}
