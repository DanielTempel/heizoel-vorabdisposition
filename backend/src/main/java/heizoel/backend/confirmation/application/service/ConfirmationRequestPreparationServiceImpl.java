package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.port.out.token.TokenService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfirmationRequestPreparationServiceImpl implements ConfirmationRequestPreparationService {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    private final OrderSnapshotRepositoryPort orderSnapshotRepository;
    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final TokenService tokenService;

    @Override
    public ConfirmationRequestCreationResult prepareConfirmationRequest(
            OrderSnapshotData orderData,
            ConfirmationRequestData requestData
    ) {
        Optional<OrderSnapshot> existingOrder =
                orderSnapshotRepository.findByExternalOrderId(orderData.externalOrderId());

        if (existingOrder.isEmpty()) {
            OrderSnapshot orderSnapshot = createOrderSnapshot(orderData);
            OrderSnapshot savedOrderSnapshot = orderSnapshotRepository.save(orderSnapshot);
            return createNewRequest(savedOrderSnapshot, requestData);
        }

        OrderSnapshot orderSnapshot = existingOrder.get();

        Optional<ConfirmationRequest> latestRequest =
                confirmationRequestRepository.findLatestByOrderSnapshot(orderSnapshot);

        if (latestRequest.isPresent()) {
            ConfirmationRequest request = latestRequest.get();

            if (isReusable(orderSnapshot, request, orderData, requestData)) {
                return new ConfirmationRequestCreationResult(
                        orderSnapshot,
                        request,
                        false
                );
            }

            if (request.isActive()) {
                request.markInactive();
                confirmationRequestRepository.save(request);
            }
        }

        OrderSnapshot updatedOrderSnapshot = updateOrderSnapshot(orderSnapshot, orderData);
        return createNewRequest(updatedOrderSnapshot, requestData);
    }

    private ConfirmationRequestCreationResult createNewRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData data
    ) {
        Instant sentAt = Instant.now();
        Instant deliveryStartsAt = data.deliveryDate()
                .atTime(data.deliveryWindowStart())
                .atZone(DELIVERY_ZONE)
                .toInstant();

        if (!deliveryStartsAt.isAfter(sentAt)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }

        Instant requestedExpiresAt =
                sentAt.plus(Duration.ofHours(data.responseDeadline()));
        Instant effectiveExpiresAt = requestedExpiresAt.isBefore(deliveryStartsAt)
                ? requestedExpiresAt
                : deliveryStartsAt;

        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                orderSnapshot,
                tokenService.generateToken(),
                data.communicationChannel(),
                data.deliveryDate(),
                data.deliveryWindowStart(),
                data.deliveryWindowEnd(),
                sentAt,
                effectiveExpiresAt,
                data.responseDeadline()
        );

        ConfirmationRequest savedConfirmationRequest =
                confirmationRequestRepository.save(confirmationRequest);

        return new ConfirmationRequestCreationResult(
                orderSnapshot,
                savedConfirmationRequest,
                true
        );
    }

    private boolean isReusable(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest latestRequest,
            OrderSnapshotData orderData,
            ConfirmationRequestData requestData
    ) {
        boolean sameOrderData = orderSnapshot.hasSameData(
                orderData.customerName(),
                orderData.customerEmail(),
                orderData.customerPhoneNumber(),
                orderData.deliveryAddress(),
                orderData.product(),
                orderData.quantityLiters(),
                orderData.priceDisplayText()
        );

        boolean sameRequestData = latestRequest.hasSameData(
                requestData.deliveryDate(),
                requestData.deliveryWindowStart(),
                requestData.deliveryWindowEnd(),
                requestData.communicationChannel(),
                requestData.responseDeadline()
        );

        boolean reusableState =
                latestRequest.isActive()
                        || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.CONFIRMED
                        || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.REJECTED;

        return sameOrderData && sameRequestData && reusableState;
    }

    private OrderSnapshot createOrderSnapshot(OrderSnapshotData data) {
        return OrderSnapshot.create(
                data.externalOrderId(),
                data.customerName(),
                data.customerEmail(),
                data.customerPhoneNumber(),
                data.deliveryAddress(),
                data.product(),
                data.quantityLiters(),
                data.priceDisplayText()
        );
    }

    private OrderSnapshot updateOrderSnapshot(
            OrderSnapshot orderSnapshot,
            OrderSnapshotData data
    ) {
        orderSnapshot.update(
                data.customerName(),
                data.customerEmail(),
                data.customerPhoneNumber(),
                data.deliveryAddress(),
                data.product(),
                data.quantityLiters(),
                data.priceDisplayText()
        );

        return orderSnapshotRepository.save(orderSnapshot);
    }

}
