package heizoel.backend.dispo.application;


import heizoel.backend.camunda.application.interfaces.NoResponseTimeoutService;
import heizoel.backend.customer.application.interfaces.CustomerResponseService;
import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.application.interfaces.OrderSnapshotService;
import heizoel.backend.dispo.application.model.command.CustomerConfirmationStatusChangedEvent;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.exceptions.customer.ConfirmationRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NoResponseTimeoutServiceImpl implements NoResponseTimeoutService {

    private final ConfirmationRequestService confirmationRequestService;
    private final OrderSnapshotService orderSnapshotService;
    private final CustomerResponseService customerResponseService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handleTimeout(Long confirmationRequestId) {
        ConfirmationRequest confirmationRequest = confirmationRequestService.findById(confirmationRequestId)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException("Confirmation request was not found."));

        if (!confirmationRequest.isActive()) {
            return;
        }

        if (customerResponseService.existsFor(confirmationRequest)) {
            return;
        }

        if (confirmationRequest.getExpiresAt().isAfter(Instant.now())) {
            return;
        }

        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        confirmationRequestService.markInactive(confirmationRequest);
        orderSnapshotService.updateStatus(orderSnapshot, ConfirmationStatus.NO_RESPONSE);

        eventPublisher.publishEvent(new CustomerConfirmationStatusChangedEvent(
                orderSnapshot.getExternalOrderId(),
                ConfirmationStatus.NO_RESPONSE,
                        null));
    }

}
