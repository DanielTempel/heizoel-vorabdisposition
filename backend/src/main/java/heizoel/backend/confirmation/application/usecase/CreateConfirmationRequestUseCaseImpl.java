package heizoel.backend.confirmation.application.usecase;


import heizoel.backend.confirmation.application.port.out.ConfirmationWorkflowService;
import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.in.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.application.port.in.CreateConfirmationRequestResult;
import heizoel.backend.confirmation.application.port.in.CreateConfirmationRequestUseCase;
import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.confirmation.domain.exception.MissingDigitalContactException;
import heizoel.backend.confirmation.application.port.out.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateConfirmationRequestUseCaseImpl implements CreateConfirmationRequestUseCase {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    private final OrderSnapshotService orderSnapshotService;
    private final ConfirmationRequestService confirmationRequestService;
    private final NotificationService notificationService;
    private final ConfirmationWorkflowService confirmationWorkflowService;

    @Override
    @Transactional
    public CreateConfirmationRequestResult createConfirmationRequest(CreateConfirmationRequestCommand command) {

        validateDeliveryWindow(command);
        validateCommunicationChannel(command);

        OrderSnapshotData orderData = new OrderSnapshotData(
                command.externalOrderId(),
                command.customerName(),
                command.customerEmail(),
                command.customerPhoneNumber(),
                command.deliveryAddress(),
                command.product(),
                command.quantityLiters(),
                command.priceDisplayText()
        );

        ConfirmationRequestData requestData = new ConfirmationRequestData(
                command.deliveryDate(),
                command.deliveryWindowStart(),
                command.deliveryWindowEnd(),
                command.communicationChannel(),
                command.responseDeadlineHours()
        );

        Optional<OrderSnapshot> existingOrder = orderSnapshotService.findByExternalOrderId(orderData.externalOrderId());
        if (existingOrder.isEmpty()) {
            OrderSnapshot orderSnapshot = orderSnapshotService.create(orderData);
            return createNewConfirmation(orderSnapshot, requestData);
        }

        OrderSnapshot orderSnapshot = existingOrder.get();
        Optional<ConfirmationRequest> latestRequest = confirmationRequestService.findLatestRequest(orderSnapshot);
        if (latestRequest.isPresent()
                && orderSnapshotService.hasSameData(orderSnapshot, orderData)
                && confirmationRequestService.hasSameData(latestRequest.get(), requestData)
                && (latestRequest.get().isActive()
                || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.CONFIRMED
                || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.REJECTED)) {

            return new CreateConfirmationRequestResult(
                    orderSnapshot.getExternalOrderId(),
                    orderSnapshot.getConfirmationStatus(),
                    false
            );
        }

        latestRequest.filter(ConfirmationRequest::isActive)
                .ifPresent(confirmationRequestService::markInactive);
        OrderSnapshot updatedOrderSnapshot = orderSnapshotService.update(orderSnapshot, orderData);


        return createNewConfirmation(updatedOrderSnapshot, requestData);
    }

    private CreateConfirmationRequestResult createNewConfirmation(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData requestData
    ) {
        ConfirmationRequest confirmationRequest =
                confirmationRequestService.create(orderSnapshot, requestData);

        notificationService.sendConfirmationRequest(orderSnapshot, confirmationRequest);

        confirmationWorkflowService.startTimeoutProcess(confirmationRequest);

        return new CreateConfirmationRequestResult(
                orderSnapshot.getExternalOrderId(),
                orderSnapshot.getConfirmationStatus(),
                true
        );
    }

    private void validateDeliveryWindow(CreateConfirmationRequestCommand command) {
        if (!command.deliveryWindowStart().isBefore(command.deliveryWindowEnd())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }

        Instant deliveryStartsAt = command.deliveryDate()
                .atTime(command.deliveryWindowStart())
                .atZone(DELIVERY_ZONE)
                .toInstant();

        if (!deliveryStartsAt.isAfter(Instant.now())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

