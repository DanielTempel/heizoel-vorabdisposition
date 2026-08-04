package heizoel.backend.application.service.workflow;

import heizoel.backend.application.port.in.workflow.SendDispoStatusCallbackCommand;
import heizoel.backend.application.port.in.workflow.SendDispoStatusCallbackUseCase;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.domain.Order;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendDispoStatusCallbackService implements SendDispoStatusCallbackUseCase {

    private final DispoStatusCallbackService dispoStatusCallbackService;
    private final OrderRepository orderRepository;

    @Override
    public void sendDispoStatusCallback(SendDispoStatusCallbackCommand command) {

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order was not found."
                ));

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
