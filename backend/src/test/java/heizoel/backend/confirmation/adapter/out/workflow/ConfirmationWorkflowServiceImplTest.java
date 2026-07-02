package heizoel.backend.confirmation.adapter.out.workflow;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationWorkflowServiceImplTest {

    @Mock
    RuntimeService runtimeService;

    @InjectMocks
    ConfirmationWorkflowServiceImpl service;

    @Test
    void startTimeoutProcess_passesAbsoluteExpirationToCamunda() {
        Instant responseDeadlineAt = Instant.parse("2099-06-12T08:00:00Z");

        service.startTimeoutProcess(42L, responseDeadlineAt);

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

