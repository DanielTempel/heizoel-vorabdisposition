package heizoel.backend.application.service.overview;

import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.model.overview.LatestConfirmationRequest;
import heizoel.backend.application.model.overview.LatestCustomerResponse;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetConfirmationDetailService implements GetConfirmationDetailUseCase {

    private final OrderRepository orderRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final CustomerResponseRepository customerResponseRepository;

    @Override
    public ConfirmationDetail getOrderDetail(GetConfirmationDetailQuery query) {
        Order order = orderRepository
                .findByCompanyIdAndExternalOrderId(
                        query.companyContext().companyId(),
                        query.externalOrderId()
                )
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order was not found."
                ));

        ConfirmationRequest latestRequest = confirmationRequestRepository
                .findTopByOrderOrderByIdDesc(order)
                .orElse(null);

        LatestConfirmationRequest latestRequestResult = latestRequest != null
                ? toLatestRequest(latestRequest)
                : null;

        LatestCustomerResponse latestCustomerResponseResult = latestRequest != null
                ? findLatestCustomerResponse(latestRequest)
                : null;

        return new ConfirmationDetail(
                order.getExternalOrderId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getProduct(),
                order.getQuantityLiters(),
                order.getPriceDisplayText(),
                order.getConfirmationStatus(),
                latestRequestResult,
                latestCustomerResponseResult
        );
    }

    private LatestCustomerResponse findLatestCustomerResponse(
            ConfirmationRequest latestRequest
    ) {
        return customerResponseRepository
                .findByConfirmationRequest(latestRequest)
                .map(this::toLatestCustomerResponse)
                .orElse(null);
    }

    private LatestConfirmationRequest toLatestRequest(ConfirmationRequest request) {
        DeliverySlot deliverySlot = request.getDeliverySlot();

        return new LatestConfirmationRequest(
                deliverySlot.getDate(),
                deliverySlot.getStart(),
                deliverySlot.getEnd(),
                request.getCommunicationChannel(),
                request.getSentAt(),
                request.getExpiresAt(),
                request.isActive()
        );
    }

    private LatestCustomerResponse toLatestCustomerResponse(CustomerResponse response) {
        return new LatestCustomerResponse(
                response.getResponseType(),
                response.getComment(),
                response.getReceivedAt()
        );
    }

}
