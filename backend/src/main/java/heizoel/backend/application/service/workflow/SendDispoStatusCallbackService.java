package heizoel.backend.application.service.workflow;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.application.port.in.workflow.SendDispoStatusCallbackCommand;
import heizoel.backend.application.port.in.workflow.SendDispoStatusCallbackUseCase;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendDispoStatusCallbackService implements SendDispoStatusCallbackUseCase {

    private final DispoStatusCallbackService dispoStatusCallbackService;
    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public void sendDispoStatusCallback(SendDispoStatusCallbackCommand command) {

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order was not found."
                ));

        ConfirmationRequest latestRequest =
                confirmationRequestRepository
                        .findTopByOrderOrderByIdDesc(order)
                        .orElseThrow(() -> new IllegalStateException(
                                "No confirmation request found for order."
                        ));

        if (!latestRequest.getId()
                .equals(command.confirmationRequestId())) {

            return;
        }

        dispoStatusCallbackService.sendStatusUpdate(
                new DispoStatusCallbackRequest(
                        order.getCompany().getCallbackUrl(),
                        order.getExternalOrderId(),
                        command.confirmationStatus(),
                        command.customerComment()
                )
        );
    }
}
