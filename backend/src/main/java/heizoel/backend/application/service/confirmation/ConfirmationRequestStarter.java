package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.application.port.out.token.TokenService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ConfirmationRequestStarter {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final TokenService tokenService;
    private final ConfirmationWorkflowService confirmationWorkflowService;

    void createAndStart(
            Order order,
            CommunicationChannel communicationChannel,
            DeliverySlot deliverySlot,
            Integer responseDeadlineHours
    ) {
        ConfirmationRequest request =
                ConfirmationRequest.createPending(
                        order,
                        tokenService.generateToken(),
                        communicationChannel,
                        deliverySlot,
                        responseDeadlineHours
                );

        ConfirmationRequest savedRequest =
                confirmationRequestRepository.save(request);

        confirmationWorkflowService.startDeliveryProcess(
                savedRequest.getId()
        );
    }



}