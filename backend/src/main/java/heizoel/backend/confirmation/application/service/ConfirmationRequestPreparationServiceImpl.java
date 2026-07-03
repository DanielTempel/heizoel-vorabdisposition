package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.port.out.token.TokenService;
import heizoel.backend.confirmation.domain.model.Company;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
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
            Company company,
            CreateConfirmationRequestCommand command
    ) {
        OrderData orderData = OrderData.from(command);
        RequestData requestData = RequestData.from(command);

        Optional<OrderSnapshot> existingOrder =
                orderSnapshotRepository.findByCompanyIdAndExternalOrderId(
                        company.getId(),
                        orderData.externalOrderId()
                );

        if (existingOrder.isEmpty()) {
            OrderSnapshot orderSnapshot = createOrderSnapshot(company, orderData);
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
            RequestData data
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
                sentAt.plus(Duration.ofHours(data.responseDeadlineHours()));

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
                data.responseDeadlineHours()
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
            OrderData orderData,
            RequestData requestData
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
                requestData.responseDeadlineHours()
        );

        boolean reusableState =
                latestRequest.isActive()
                        || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.CONFIRMED
                        || orderSnapshot.getConfirmationStatus() == ConfirmationStatus.REJECTED;

        return sameOrderData && sameRequestData && reusableState;
    }

    private OrderSnapshot createOrderSnapshot(
            Company company,
            OrderData data
    ) {
        return OrderSnapshot.create(
                company,
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
            OrderData data
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


    private record OrderData(
            String externalOrderId,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        static OrderData from(CreateConfirmationRequestCommand command) {
            return new OrderData(
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
    }

    private record RequestData(
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            LocalTime deliveryWindowEnd,
            CommunicationChannel communicationChannel,
            Integer responseDeadlineHours
    ) {
        static RequestData from(CreateConfirmationRequestCommand command) {
            return new RequestData(
                    command.deliveryDate(),
                    command.deliveryWindowStart(),
                    command.deliveryWindowEnd(),
                    command.communicationChannel(),
                    command.responseDeadlineHours()
            );
        }
    }

}

