package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.exception.ConfirmationRequestResendNotAllowedException;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResendConfirmationRequestService implements ResendConfirmationRequestUseCase {


    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final ConfirmationRequestStarter confirmationRequestStarter;
    private final Clock clock;

    @Override
    @Transactional
    public ResendConfirmationRequestResult resend(ResendConfirmationRequestCommand command) {

        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(
                        command.companyContext().companyId(),
                        command.externalOrderId()
                )
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order was not found."
                        )
                );

        validateCommunicationChannel(order, command.communicationChannel());

        ConfirmationRequest previousRequest =
                confirmationRequestRepository
                        .findTopByOrderOrderByIdDesc(order)
                        .orElseThrow(() ->
                                new ConfirmationRequestNotFoundException(
                                        "Confirmation request was not found."
                                )
                        );

        validateResendAllowed(order, previousRequest);

        previousRequest.getDeliverySlot().validateStartsAfter(Instant.now(clock));

        order.markOpen();

        confirmationRequestStarter.createAndStart(
                order,
                command.communicationChannel(),
                previousRequest.getDeliverySlot(),
                command.responseDeadlineHours()
        );

        return new ResendConfirmationRequestResult(
                order.getExternalOrderId(),
                order.getConfirmationStatus()
        );
    }

    private void validateResendAllowed(
            Order order,
            ConfirmationRequest previousRequest
    ) {
        boolean afterDeliveryFailure =
                previousRequest.isDeliveryFailed()
                        && !previousRequest.isActive();

        boolean afterNoResponse =
                order.getConfirmationStatus() == ConfirmationStatus.NO_RESPONSE
                        && previousRequest.isSent()
                        && !previousRequest.isActive();

        if (!afterDeliveryFailure && !afterNoResponse) {
            throw new ConfirmationRequestResendNotAllowedException(
                    "Confirmation request cannot be resent in the current state."
            );
        }
    }

    private void validateCommunicationChannel(
            Order order,
            CommunicationChannel communicationChannel
    ) {
        if (communicationChannel == CommunicationChannel.EMAIL
                && isBlank(order.getCustomerEmail())) {
            throw new MissingDigitalContactException(
                    "Customer e-mail is required when communication channel is EMAIL."
            );
        }

        if ((communicationChannel == CommunicationChannel.SMS
                || communicationChannel == CommunicationChannel.WHATSAPP)
                && isBlank(order.getCustomerPhoneNumber())) {
            throw new MissingDigitalContactException(
                    "Customer phone number is required when communication channel is "
                            + communicationChannel + "."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }


}
