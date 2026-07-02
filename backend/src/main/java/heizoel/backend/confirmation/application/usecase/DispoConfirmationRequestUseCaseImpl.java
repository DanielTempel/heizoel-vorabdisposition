package heizoel.backend.confirmation.application.usecase;


import heizoel.backend.confirmation.application.port.out.*;
import heizoel.backend.confirmation.application.port.in.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.application.port.in.CreateConfirmationRequestResult;
import heizoel.backend.confirmation.application.port.in.DispoConfirmationRequestUseCase;
import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.application.service.ConfirmationRequestPreparationService;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.confirmation.domain.exception.MissingDigitalContactException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispoConfirmationRequestUseCaseImpl implements DispoConfirmationRequestUseCase {

    private final ConfirmationRequestPreparationService confirmationRequestPreparationService;
    private final NotificationService notificationService;
    private final ConfirmationWorkflowService confirmationWorkflowService;

    @Override
    @Transactional
    public CreateConfirmationRequestResult createConfirmationRequest(CreateConfirmationRequestCommand command) {

        validateDeliveryWindow(command);
        validateCommunicationChannel(command);

        OrderSnapshotData orderData = toOrderSnapshotData(command);
        ConfirmationRequestData requestData = toConfirmationRequestData(command);

        ConfirmationRequestCreationResult creationResult =
                confirmationRequestPreparationService.prepareConfirmationRequest(
                        orderData,
                        requestData
                );

        if (creationResult.created()) {
            notificationService.sendConfirmationRequest(
                    creationResult.orderSnapshot(),
                    creationResult.confirmationRequest()
            );
            confirmationWorkflowService.startTimeoutProcess(creationResult.confirmationRequest());
        }

        return new CreateConfirmationRequestResult(
                creationResult.orderSnapshot().getExternalOrderId(),
                creationResult.orderSnapshot().getConfirmationStatus(),
                creationResult.created()
        );
    }

    private void validateDeliveryWindow(CreateConfirmationRequestCommand command) {
        if (!command.deliveryWindowStart().isBefore(command.deliveryWindowEnd())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }
    }

    private void validateCommunicationChannel(CreateConfirmationRequestCommand command) {
        if (command.communicationChannel() == CommunicationChannel.EMAIL
                && isBlank(command.customerEmail())) {
            throw new MissingDigitalContactException(
                    "Customer e-mail is required when communication channel is EMAIL."
            );
        }

        if (command.communicationChannel() == CommunicationChannel.SMS
                && isBlank(command.customerPhoneNumber())) {
            throw new MissingDigitalContactException(
                    "Customer phone number is required when communication channel is SMS."
            );
        }
    }

    private OrderSnapshotData toOrderSnapshotData(CreateConfirmationRequestCommand command) {
        return new OrderSnapshotData(
                command.externalOrderId(),
                command.customerName(),
                command.customerEmail(),
                command.customerPhoneNumber(),
                command.deliveryAddress(),
                command.product(),
                command.quantityLiters(),
                command.priceDisplayText()
        );
    }

    private ConfirmationRequestData toConfirmationRequestData(
            CreateConfirmationRequestCommand command
    ) {
        return new ConfirmationRequestData(
                command.deliveryDate(),
                command.deliveryWindowStart(),
                command.deliveryWindowEnd(),
                command.communicationChannel(),
                command.responseDeadlineHours()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

