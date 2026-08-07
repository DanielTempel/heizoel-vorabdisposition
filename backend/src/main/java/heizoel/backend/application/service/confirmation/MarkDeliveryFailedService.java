package heizoel.backend.application.service.confirmation;


import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.port.in.workflow.MarkDeliveryFailedUseCase;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkDeliveryFailedService implements MarkDeliveryFailedUseCase {

    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;

    @Override
    @Transactional
    public void markDeliveryFailed(Long confirmationRequestId) {

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
         * Idempotency protection in case the Camunda job
         * is executed again after FAILED was already persisted.
         */
        if (request.isDeliveryFailed()) {
            return;
        }

        if (!request.isPending()) {
            throw new IllegalStateException(
                    "Only a pending confirmation request can be marked as failed."
            );
        }

        request.markDeliveryFailed();
        order.markOpen();
    }

}
