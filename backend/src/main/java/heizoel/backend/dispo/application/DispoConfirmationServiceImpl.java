package heizoel.backend.dispo.application;


import heizoel.backend.camunda.application.interfaces.ConfirmationWorkflowService;
import heizoel.backend.dispo.api.dto.request.DispoConfirmationRequestDto;
import heizoel.backend.dispo.api.dto.response.DispoConfirmationResponseDto;
import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.application.interfaces.DispoConfirmationService;
import heizoel.backend.dispo.application.interfaces.OrderSnapshotService;
import heizoel.backend.dispo.application.model.ConfirmationRequestData;
import heizoel.backend.dispo.application.model.OrderSnapshotData;
import heizoel.backend.dispo.application.model.command.DispoConfirmationCreationResult;
import heizoel.backend.notification.domain.CommunicationChannel;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.exceptions.dispo.InvalidDeliveryWindowException;
import heizoel.backend.exceptions.dispo.MissingDigitalContactException;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DispoConfirmationServiceImpl implements DispoConfirmationService {

    private final OrderSnapshotService orderSnapshotService;
    private final ConfirmationRequestService confirmationRequestService;
    private final ConfirmationNotificationService notificationService;
    private final ConfirmationWorkflowService confirmationWorkflowService;

    @Override
    @Transactional
    public DispoConfirmationCreationResult createConfirmationRequest(DispoConfirmationRequestDto request) {

        if (!request.deliveryWindowStart().isBefore(request.deliveryWindowEnd())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }

        validateCommunicationChannel(request);

        OrderSnapshotData orderData = new OrderSnapshotData(
                request.externalOrderId(),
                request.customerName(),
                request.customerEmail(),
                request.customerPhoneNumber(),
                request.deliveryAddress(),
                request.product(),
                request.quantityLiters()
        );

        ConfirmationRequestData requestData = new ConfirmationRequestData(
                request.deliveryDate(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.communicationChannel()
        );

        Optional<OrderSnapshot> existingOrder = orderSnapshotService.findByExternalOrderId(orderData.externalOrderId());
        if (existingOrder.isEmpty()) {
            OrderSnapshot orderSnapshot = orderSnapshotService.create(orderData);
            return createNewConfirmation(orderSnapshot, requestData);
        }

        OrderSnapshot orderSnapshot = existingOrder.get();
        Optional<ConfirmationRequest> activeRequest = confirmationRequestService.findActiveRequest(orderSnapshot);
        if (activeRequest.isPresent()
                && orderSnapshotService.hasSameData(orderSnapshot, orderData)
                && confirmationRequestService.hasSameData(activeRequest.get(), requestData)) {

            return new DispoConfirmationCreationResult(
                    new DispoConfirmationResponseDto(
                            orderSnapshot.getExternalOrderId(),
                            ConfirmationStatus.SENT
                    ),
                    false
            );
        }

        activeRequest.ifPresent(confirmationRequestService::markInactive);
        OrderSnapshot updatedOrderSnapshot = orderSnapshotService.update(orderSnapshot, orderData);




        return createNewConfirmation(updatedOrderSnapshot, requestData);
    }

    private DispoConfirmationCreationResult createNewConfirmation(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData requestData
    ) {
        ConfirmationRequest confirmationRequest =
                confirmationRequestService.create(orderSnapshot, requestData);

        notificationService.sendConfirmationRequest(orderSnapshot, confirmationRequest);

        confirmationWorkflowService.startTimeoutProcess(confirmationRequest);

        return new DispoConfirmationCreationResult(
                new DispoConfirmationResponseDto(
                        orderSnapshot.getExternalOrderId(),
                        ConfirmationStatus.SENT
                ),
                true
        );
    }

    private void validateCommunicationChannel(DispoConfirmationRequestDto request) {
        if (request.communicationChannel() == CommunicationChannel.EMAIL
                && isBlank(request.customerEmail())) {
            throw new MissingDigitalContactException(
                    "Customer e-mail is required when communication channel is EMAIL."
            );
        }

        if (request.communicationChannel() == CommunicationChannel.SMS
                && isBlank(request.customerPhoneNumber())) {
            throw new MissingDigitalContactException(
                    "Customer phone number is required when communication channel is SMS."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
