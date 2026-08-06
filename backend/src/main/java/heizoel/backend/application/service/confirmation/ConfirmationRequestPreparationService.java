package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.ConfirmationRequestCreationResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.out.token.TokenService;
import heizoel.backend.domain.*;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.domain.company.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfirmationRequestPreparationService  {

    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final TokenService tokenService;
    private final Clock clock;


    public ConfirmationRequestCreationResult prepareConfirmationRequest(
            Company company,
            CreateConfirmationRequestCommand command
    ) {
        OrderData orderData = OrderData.from(command);
        RequestData requestData = RequestData.from(command);

        Optional<Order> existingOrder =
                orderRepository.findByCompanyIdAndExternalOrderId(
                        company.getId(),
                        orderData.externalOrderId()
                );

        if (existingOrder.isEmpty()) {
            Order order = createOrderSnapshot(company, orderData);
            Order savedOrder = orderRepository.save(order);
            return createNewRequest(savedOrder, requestData);
        }

        Order order = existingOrder.get();

        Optional<ConfirmationRequest> latestRequest =
                confirmationRequestRepository.findTopByOrderOrderByIdDesc(order);

        if (latestRequest.isPresent()) {
            ConfirmationRequest request = latestRequest.get();

            if (isReusable(order, request, orderData, requestData)) {
                return new ConfirmationRequestCreationResult(
                        order,
                        request,
                        false
                );
            }

            if (request.isActive()) {
                request.markInactive();
                confirmationRequestRepository.save(request);
            }
        }

        Order updatedOrder = updateOrderSnapshot(order, orderData);
        return createNewRequest(updatedOrder, requestData);
    }

    private ConfirmationRequestCreationResult createNewRequest(
            Order order,
            RequestData data
    ) {
        Instant sentAt = Instant.now(clock);

        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                order,
                tokenService.generateToken(),
                data.communicationChannel(),
                data.deliverySlot(),
                sentAt,
                data.responseDeadlineHours()
        );

        ConfirmationRequest savedConfirmationRequest =
                confirmationRequestRepository.save(confirmationRequest);

        return new ConfirmationRequestCreationResult(
                order,
                savedConfirmationRequest,
                true
        );
    }

    private boolean isReusable(
            Order order,
            ConfirmationRequest latestRequest,
            OrderData orderData,
            RequestData requestData
    ) {
        boolean sameOrderData = order.hasSameData(
                orderData.tour(),
                orderData.customerName(),
                orderData.customerEmail(),
                orderData.customerPhoneNumber(),
                orderData.deliveryAddress(),
                orderData.product(),
                orderData.quantityLiters(),
                orderData.priceDisplayText()
        );

        boolean sameRequestData = latestRequest.hasSameData(
                requestData.deliverySlot(),
                requestData.communicationChannel(),
                requestData.responseDeadlineHours()
        );

        boolean reusableState =
                latestRequest.isActive()
                        || order.getConfirmationStatus() == ConfirmationStatus.CONFIRMED
                        || order.getConfirmationStatus() == ConfirmationStatus.REJECTED;

        return sameOrderData && sameRequestData && reusableState;
    }

    private Order createOrderSnapshot(
            Company company,
            OrderData data
    ) {
        return Order.create(
                company,
                data.externalOrderId(),
                data.tour(),
                data.customerName(),
                data.customerEmail(),
                data.customerPhoneNumber(),
                data.deliveryAddress(),
                data.product(),
                data.quantityLiters(),
                data.priceDisplayText()
        );
    }

    private Order updateOrderSnapshot(
            Order order,
            OrderData data
    ) {
        order.update(
                data.tour(),
                data.customerName(),
                data.customerEmail(),
                data.customerPhoneNumber(),
                data.deliveryAddress(),
                data.product(),
                data.quantityLiters(),
                data.priceDisplayText()
        );

        return orderRepository.save(order);
    }


    private record OrderData(
            String externalOrderId,
            Tour tour,
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
                    Tour.of(
                            command.tourNumber(),
                            command.vehicleLicensePlate()
                    ),
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
            DeliverySlot deliverySlot,
            CommunicationChannel communicationChannel,
            Integer responseDeadlineHours
    ) {
        static RequestData from(CreateConfirmationRequestCommand command) {
            return new RequestData(
                    DeliverySlot.of(
                            command.deliveryDate(),
                            command.deliveryWindowStart(),
                            command.deliveryWindowEnd()
                    ),
                    command.communicationChannel(),
                    command.responseDeadlineHours()
            );
        }
    }

}
