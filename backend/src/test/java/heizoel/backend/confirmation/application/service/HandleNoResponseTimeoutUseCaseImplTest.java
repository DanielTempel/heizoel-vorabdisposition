package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.DispoCallbackWorkflowService;
import heizoel.backend.confirmation.application.port.out.CustomerResponseService;
import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.application.usecase.HandleNoResponseTimeoutUseCaseImpl;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleNoResponseTimeoutUseCaseImplTest {

    @Mock
    ConfirmationRequestService confirmationRequestService;

    @Mock
    OrderSnapshotService orderSnapshotService;

    @Mock
    CustomerResponseService customerResponseService;

    @Mock
    DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @InjectMocks
    HandleNoResponseTimeoutUseCaseImpl service;

    @Test
    void handleTimeout_doesNothingWhenRequestIsInactive() {
        ConfirmationRequest confirmationRequest = confirmationRequest(false, Instant.now().minusSeconds(1));

        when(confirmationRequestService.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));

        service.handleTimeout(1L);

        verifyNoInteractions(customerResponseService, orderSnapshotService, dispoCallbackWorkflowService);
        verify(confirmationRequestService, never()).markInactive(any());
    }

    @Test
    void handleTimeout_doesNothingWhenCustomerResponseAlreadyExists() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().minusSeconds(1));

        when(confirmationRequestService.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseService.existsFor(confirmationRequest))
                .thenReturn(true);

        service.handleTimeout(1L);

        verify(customerResponseService).existsFor(confirmationRequest);
        verifyNoInteractions(orderSnapshotService, dispoCallbackWorkflowService);
        verify(confirmationRequestService, never()).markInactive(any());
    }

    @Test
    void handleTimeout_doesNothingWhenDeadlineIsStillInTheFuture() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().plusSeconds(60));

        when(confirmationRequestService.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseService.existsFor(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(customerResponseService).existsFor(confirmationRequest);
        verifyNoInteractions(orderSnapshotService, dispoCallbackWorkflowService);
        verify(confirmationRequestService, never()).markInactive(any());
    }

    @Test
    void handleTimeout_marksNoResponseWhenRequestIsActiveUnansweredAndExpired() {
        ConfirmationRequest confirmationRequest = confirmationRequest(true, Instant.now().minusSeconds(1));
        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        when(confirmationRequestService.findById(1L))
                .thenReturn(Optional.of(confirmationRequest));
        when(customerResponseService.existsFor(confirmationRequest))
                .thenReturn(false);

        service.handleTimeout(1L);

        verify(confirmationRequestService).markInactive(confirmationRequest);
        verify(orderSnapshotService).updateStatus(orderSnapshot, ConfirmationStatus.NO_RESPONSE);
        verify(dispoCallbackWorkflowService).startDispoCallbackProcess(
                "A-TIMEOUT-1",
                ConfirmationStatus.NO_RESPONSE,
                null
        );
    }

    @Test
    void handleTimeout_throwsNotFoundWhenRequestDoesNotExist() {
        when(confirmationRequestService.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleTimeout(1L))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(customerResponseService, orderSnapshotService, dispoCallbackWorkflowService);
    }

    private ConfirmationRequest confirmationRequest(boolean active, Instant expiresAt) {
        OrderSnapshot orderSnapshot = new OrderSnapshot();
        orderSnapshot.setExternalOrderId("A-TIMEOUT-1");

        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.setOrderSnapshot(orderSnapshot);
        confirmationRequest.setActive(active);
        confirmationRequest.setExpiresAt(expiresAt);

        return confirmationRequest;
    }
}

