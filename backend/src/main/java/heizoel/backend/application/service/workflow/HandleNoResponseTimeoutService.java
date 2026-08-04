package heizoel.backend.application.service.workflow;

import heizoel.backend.application.port.in.workflow.HandleNoResponseTimeoutUseCase;
import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
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
    private final CustomerResponseRepository customerResponseRepository;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;
    private final Clock clock;

    @Override
    @Transactional
    public void handleTimeout(Long confirmationRequestId) {

        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findById(confirmationRequestId)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException("Confirmation request was not found."));

        if (!confirmationRequest.isActive()) {
            return;
        }

        if (customerResponseRepository.existsByConfirmationRequest(confirmationRequest)) {
            return;
        }

        if (confirmationRequest.getExpiresAt().isAfter(Instant.now(clock))) {
            return;
        }

        Order order = confirmationRequest.getOrder();

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);
        order.markNoResponse();
        orderRepository.save(order);

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                order.getId(),
                ConfirmationStatus.NO_RESPONSE,
                null
        );
    }

}

