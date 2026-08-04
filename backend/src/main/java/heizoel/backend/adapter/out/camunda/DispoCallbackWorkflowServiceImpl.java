package heizoel.backend.adapter.out.camunda;


import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.domain.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DispoCallbackWorkflowServiceImpl  implements DispoCallbackWorkflowService {

    private static final String PROCESS_KEY = "dispo-callback-process";

    private static final String VAR_ORDER_ID = "orderSnapshotId";
    private static final String VAR_CONFIRMATION_STATUS = "confirmationStatus";
    private static final String VAR_CUSTOMER_COMMENT = "customerComment";

    private final RuntimeService runtimeService;

    @Override
    public void startDispoCallbackProcess(
            Long orderId,
            ConfirmationStatus confirmationStatus,
            String customerComment
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_ORDER_ID, orderId);
        variables.put(VAR_CONFIRMATION_STATUS, confirmationStatus.name());
        variables.put(VAR_CUSTOMER_COMMENT, customerComment);

        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                orderId.toString(),
                variables
        );
    }
}

