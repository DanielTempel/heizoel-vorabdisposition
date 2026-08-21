package heizoel.backend.application.service.confirmation;


import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestUseCase;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.*;
import heizoel.backend.domain.company.Company;
import heizoel.backend.application.exception.CompanyNotFoundException;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateConfirmationRequestService implements CreateConfirmationRequestUseCase {

    private final CompanyRepository companyRepository;
    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final ConfirmationWorkflowService confirmationWorkflowService;
    private final ConfirmationRequestStarter confirmationRequestStarter;
    private final Clock clock;

    @Override
    @Transactional
    public CreateConfirmationRequestResult createConfirmationRequest(CreateConfirmationRequestCommand command) {

        validateCommunicationChannel(command);

        Company company = companyRepository
                .findById(command.companyContext().companyId())
                .orElseThrow(() -> new CompanyNotFoundException("Company was not found."));

        OrderData orderData = OrderData.from(command);
        RequestData requestData = RequestData.from(command);

        Instant now = Instant.now(clock);

        if (!requestData.deliverySlot().startsAt().isAfter(now)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }

        Optional<Order> existingOrder =
                orderRepository
                        .findByCompanyIdAndExternalOrderId(
                                company.getId(),
                                orderData.externalOrderId()
                        );

        /*
         * New Order:
         * create Order = OPEN,
         * create ConfirmationRequest = PENDING,
         * start delivery process.
         */
        if (existingOrder.isEmpty()) {
            Order order = Order.create(
                    company,
                    orderData.externalOrderId(),
                    orderData.tour(),
                    orderData.customerName(),
                    orderData.customerEmail(),
                    orderData.customerPhoneNumber(),
                    orderData.deliveryAddress(),
                    orderData.product(),
                    orderData.quantityLiters(),
                    orderData.priceDisplayText()
            );
            Order savedOrder = orderRepository.save(order);

            confirmationRequestStarter.createAndStart(
                    savedOrder,
                    requestData.communicationChannel(),
                    requestData.deliverySlot(),
                    requestData.responseDeadlineHours()
            );

            return new CreateConfirmationRequestResult(
                    savedOrder.getExternalOrderId(),
                    savedOrder.getConfirmationStatus()
            );

        }

        Order order = existingOrder.get();

        Optional<ConfirmationRequest> latestRequest =
                confirmationRequestRepository
                        .findTopByOrderOrderByIdDesc(order);


        /*
         * The order exists, but there are no requests yet.
         */
        if (latestRequest.isEmpty()) {
            updateOrder(order, orderData);
            order.markOpen();

            confirmationRequestStarter.createAndStart(
                    order,
                    requestData.communicationChannel(),
                    requestData.deliverySlot(),
                    requestData.responseDeadlineHours()
            );

            return new CreateConfirmationRequestResult(
                    order.getExternalOrderId(),
                    order.getConfirmationStatus()
            );
        }

        ConfirmationRequest request = latestRequest.get();

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

        boolean sameRequestData = request.hasSameData(
                requestData.deliverySlot(),
                requestData.communicationChannel(),
                requestData.responseDeadlineHours()
        );


        /*
         * Identical data + PENDING:
         * make no changes,
         * do not start the second process.
         */
        if (request.isPending()
                && sameOrderData
                && sameRequestData) {
            return new CreateConfirmationRequestResult(
                    order.getExternalOrderId(),
                    order.getConfirmationStatus()
            );
        }

        /*
         * Identical data + an already successfully submitted
         * active/confirmed/rejected request:
         * reuse the existing result.
         */
        if (sameOrderData
                && sameRequestData
                && isReusableCompletedRequest(order, request)) {
            return new CreateConfirmationRequestResult(
                    order.getExternalOrderId(),
                    order.getConfirmationStatus()
            );
        }

        /*
         * Beyond this point, either the data has changed,
         * the previous request resulted in FAILED/NO_RESPONSE,
         * or resubmission is permitted.
         */
        updateOrder(order, orderData);
        order.markOpen();

        /*
         * Modified data + PENDING:
         * update the existing pending request.
         *
         * No new process is started:
         * a process already exists for this ConfirmationRequest.
         */
        if (request.isPending()) {
            request.updatePending(
                    requestData.communicationChannel(),
                    requestData.deliverySlot(),
                    requestData.responseDeadlineHours()
            );

            return new CreateConfirmationRequestResult(
                    order.getExternalOrderId(),
                    order.getConfirmationStatus()
            );
        }

        /*
         * An old SENT request must no longer accept responses
         * when data changes.
         */
        if (request.isActive()) {
            request.markInactive();
            confirmationWorkflowService.notifyConfirmationRequestSuperseded(request.getId());
        }

        /*
         * FAILED, NO_RESPONSE, or modified SENT request:
         * create a new PENDING request and a new Camunda process.
         */
        confirmationRequestStarter.createAndStart(
                order,
                requestData.communicationChannel(),
                requestData.deliverySlot(),
                requestData.responseDeadlineHours()
        );

        return new CreateConfirmationRequestResult(
                order.getExternalOrderId(),
                order.getConfirmationStatus()
        );
    }

    private void validateCommunicationChannel(CreateConfirmationRequestCommand command) {
        if (command.communicationChannel() == CommunicationChannel.EMAIL
                && isBlank(command.customerEmail())) {
            throw new MissingDigitalContactException(
                    "Customer e-mail is required when communication channel is EMAIL."
            );
        }

        if ((command.communicationChannel() == CommunicationChannel.SMS
                || command.communicationChannel() == CommunicationChannel.WHATSAPP)
                && isBlank(command.customerPhoneNumber())) {
            throw new MissingDigitalContactException(
                    "Customer phone number is required when communication channel is "
                            + command.communicationChannel() + "."
            );
        }
    }

    private boolean isReusableCompletedRequest(
            Order order,
            ConfirmationRequest request
    ) {
        if (!request.isSent()) {
            return false;
        }

        if (request.isActive()) {
            return true;
        }

        return order.getConfirmationStatus()
                == ConfirmationStatus.CONFIRMED
                || order.getConfirmationStatus()
                == ConfirmationStatus.REJECTED;
    }

    private void updateOrder(
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
    private static OrderData from(
            CreateConfirmationRequestCommand command
    ) {
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

    private static RequestData from(
            CreateConfirmationRequestCommand command
    ) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

