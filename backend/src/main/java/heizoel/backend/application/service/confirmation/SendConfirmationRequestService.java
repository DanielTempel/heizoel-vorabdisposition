package heizoel.backend.application.service.confirmation;


import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestResult;
import heizoel.backend.application.port.in.workflow.SendConfirmationRequestUseCase;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SendConfirmationRequestService implements SendConfirmationRequestUseCase {


    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final Clock clock;


    @Override
    @Transactional
    public SendConfirmationRequestResult send(Long confirmationRequestId) {

        Order order = orderRepository
                .findByConfirmationRequestIdForUpdate(
                        confirmationRequestId
                )
                .orElseThrow(() ->
                        new ConfirmationRequestNotFoundException(
                                "Confirmation request was not found."
                        )
                );

        ConfirmationRequest request =
                confirmationRequestRepository
                        .findById(confirmationRequestId)
                        .orElseThrow(() ->
                                new ConfirmationRequestNotFoundException(
                                        "Confirmation request was not found."
                                )
                        );


        /*
         * Idempotency protection.
         * A repeated Camunda execution must not send again
         * if SENT was already persisted.
         */
        if (request.isSent()) {
            return SendConfirmationRequestResult.sent(
                    request.getExpiresAt()
            );
        }

        if (!request.isPending()) {
            throw new IllegalStateException(
                    "Only a pending confirmation request can be sent."
            );
        }

        /*
         * Do not send a confirmation request for a delivery
         * window which is already in the past.
         */
        Instant now = Instant.now(clock);

        try {
            request.getDeliverySlot().validateStartsAfter(now);
        } catch (InvalidDeliveryWindowException exception) {
            return SendConfirmationRequestResult.permanentFailure();
        }

        try {
            notificationService.sendConfirmationRequest(
                    order,
                    request
            );
        } catch (EmailSettingsNotConfiguredException exception) {
            return SendConfirmationRequestResult
                    .permanentFailure();

        } catch (NotificationDeliveryException exception) {
            return SendConfirmationRequestResult
                    .retryableFailure();
        }


        Instant sentAt = Instant.now(clock);

        request.markSent(sentAt);
        order.markSent();

        return SendConfirmationRequestResult.sent(request.getExpiresAt());

    }
}
