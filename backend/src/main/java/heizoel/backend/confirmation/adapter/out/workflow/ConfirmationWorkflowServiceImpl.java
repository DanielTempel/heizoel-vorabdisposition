package heizoel.backend.confirmation.adapter.out.workflow;


import heizoel.backend.confirmation.application.port.out.ConfirmationWorkflowService;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfirmationWorkflowServiceImpl implements ConfirmationWorkflowService {

    private static final String PROCESS_KEY = "confirmation-timeout-process";
    private static final String VAR_CONFIRMATION_REQUEST_ID = "confirmationRequestId";
    private static final String VAR_RESPONSE_DEADLINE_AT = "responseDeadlineAt";

    private final RuntimeService runtimeService;

    @Override
    public void startTimeoutProcess(ConfirmationRequest confirmationRequest) {
        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                confirmationRequest.getId().toString(),
                Map.of(
                        VAR_CONFIRMATION_REQUEST_ID, confirmationRequest.getId(),
                        VAR_RESPONSE_DEADLINE_AT, confirmationRequest.getExpiresAt().toString()
                )
        );
    }
}

