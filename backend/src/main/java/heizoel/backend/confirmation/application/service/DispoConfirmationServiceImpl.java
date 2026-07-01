package heizoel.backend.confirmation.application.service;


import heizoel.backend.confirmation.application.port.out.ConfirmationWorkflowService;
import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;
import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.in.DispoConfirmationService;
import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.application.model.DispoConfirmationCreationResult;
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
public class DispoConfirmationServiceImpl implements DispoConfirmationService {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    private final OrderSnapshotService orderSnapshotService;
    private final ConfirmationRequestService confirmationRequestService;
    private final NotificationService notificationService;
    private final ConfirmationWorkflowService confirmationWorkflowService;

    @Override
    @Transactional
    public DispoConfirmationCreationResult createConfirmationRequest(DispoConfirmationRequestDto request) {

        validateDeliveryWindow(request);
        validateCommunicationChannel(request);

        OrderSnapshotData orderData = new OrderSnapshotData(
                request.externalOrderId(),
                request.customerName(),
                request.customerEmail(),
                request.customerPhoneNumber(),
                request.deliveryAddress(),
                request.product(),
                request.quantityLiters(),
                request.priceDisplayText()
        );

        ConfirmationRequestData requestData = new ConfirmationRequestData(
                request.deliveryDate(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.communicationChannel(),
                request.responseDeadlineHours()
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

            return new DispoConfirmationCreationResult(
                    new DispoConfirmationResponseDto(
                            orderSnapshot.getExternalOrderId(),
                            orderSnapshot.getConfirmationStatus()
                    ),
                    false
            );
        }

        latestRequest.filter(ConfirmationRequest::isActive)
                .ifPresent(confirmationRequestService::markInactive);
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
                        orderSnapshot.getConfirmationStatus()
                ),
                true
        );
    }

    private void validateDeliveryWindow(DispoConfirmationRequestDto request) {
        if (!request.deliveryWindowStart().isBefore(request.deliveryWindowEnd())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }

        Instant deliveryStartsAt = request.deliveryDate()
                .atTime(request.deliveryWindowStart())
                .atZone(DELIVERY_ZONE)
                .toInstant();

        if (!deliveryStartsAt.isAfter(Instant.now())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }
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

