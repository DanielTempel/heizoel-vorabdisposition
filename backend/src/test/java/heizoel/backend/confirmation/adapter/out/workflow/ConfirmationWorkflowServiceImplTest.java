package heizoel.backend.confirmation.adapter.out.workflow;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmationWorkflowServiceImplTest {

    @Mock
    RuntimeService runtimeService;

    @InjectMocks
    ConfirmationWorkflowServiceImpl service;

    @Test
    void startTimeoutProcess_passesAbsoluteExpirationToCamunda() {
        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.setId(42L);
        confirmationRequest.setExpiresAt(Instant.parse("2099-06-12T08:00:00Z"));
        confirmationRequest.setResponseDeadlineHours(168);

        service.startTimeoutProcess(confirmationRequest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(runtimeService).startProcessInstanceByKey(
                org.mockito.ArgumentMatchers.eq("confirmation-timeout-process"),
                org.mockito.ArgumentMatchers.eq("42"),
                variablesCaptor.capture()
        );

        assertThat(variablesCaptor.getValue())
                .containsEntry("confirmationRequestId", 42L)
                .containsEntry("responseDeadlineAt", "2099-06-12T08:00:00Z")
                .doesNotContainKey("responseDeadline");
    }
}

