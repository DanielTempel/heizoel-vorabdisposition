package heizoel.backend.application.service.workflow;

import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HandleNoResponseTimeoutService implements HandleNoResponseTimeoutUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Long handleTimeout(Long confirmationRequestId) {

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

        if (!request.isActive()) {
            throw new IllegalStateException(
                    "Only an active confirmation request can time out."
            );
        }

        Instant now = Instant.now(clock);

        if (!request.isExpiredAt(now)) {
            throw new IllegalStateException(
                    "Confirmation request has not expired yet."
            );
        }

        request.markInactive();
        order.markNoResponse();

        return order.getId();
    }

}

