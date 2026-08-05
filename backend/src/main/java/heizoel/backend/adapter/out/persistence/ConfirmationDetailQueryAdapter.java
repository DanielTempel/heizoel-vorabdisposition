package heizoel.backend.adapter.out.persistence;


import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.CustomerResponseDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.OrderDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.RequestDetail;
import heizoel.backend.application.port.out.persistence.ConfirmationDetailQueryPort;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.QConfirmationRequest;
import heizoel.backend.domain.QCustomerResponse;
import heizoel.backend.domain.QOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConfirmationDetailQueryAdapter implements ConfirmationDetailQueryPort {

    private final JPAQueryFactory queryFactory;


    @Override
    public Optional<ConfirmationDetail> findDetail(
            Long companyId,
            String externalOrderId
    ) {
        QOrder order = QOrder.order;

        Tuple orderRow = queryFactory
                .select(
                        order.id,
                        order.externalOrderId,
                        order.customerName,
                        order.customerEmail,
                        order.customerPhoneNumber,
                        order.deliveryAddress,
                        order.product,
                        order.quantityLiters,
                        order.priceDisplayText,
                        order.tour.tourNumber,
                        order.tour.vehicleLicensePlate,
                        order.confirmationStatus
                )
                .from(order)
                .where(
                        order.company.id.eq(companyId),
                        order.externalOrderId.eq(externalOrderId)
                )
                .fetchOne();

        if (orderRow == null) {
            return Optional.empty();
        }

        Long orderId = orderRow.get(order.id);

        OrderDetail orderDetail = new ConfirmationDetail.OrderDetail(
                orderRow.get(order.externalOrderId),
                orderRow.get(order.customerName),
                orderRow.get(order.customerEmail),
                orderRow.get(order.customerPhoneNumber),
                orderRow.get(order.deliveryAddress),
                orderRow.get(order.product),
                orderRow.get(order.quantityLiters),
                orderRow.get(order.priceDisplayText),
                orderRow.get(order.tour.tourNumber),
                orderRow.get(order.tour.vehicleLicensePlate),
                orderRow.get(order.confirmationStatus)
        );

        List<RequestDetail> requests = findRequests(orderId);


        RequestDetail currentRequest = requests.isEmpty()
                ? null
                : requests.get(0);

        List<RequestDetail> previousRequests = requests.size() <= 1
                ? List.of()
                : List.copyOf(
                requests.subList(1, requests.size())
        );

        return Optional.of(
                new ConfirmationDetail(
                        orderDetail,
                        currentRequest,
                        previousRequests
                )
        );
    }

    private List<RequestDetail> findRequests(Long orderId) {
        QConfirmationRequest confirmationRequest =
                QConfirmationRequest.confirmationRequest;

        QCustomerResponse customerResponse =
                QCustomerResponse.customerResponse;

        List<Tuple> rows = queryFactory
                .select(
                        confirmationRequest.id,
                        confirmationRequest.communicationChannel,
                        confirmationRequest.deliverySlot.date,
                        confirmationRequest.deliverySlot.start,
                        confirmationRequest.deliverySlot.end,
                        confirmationRequest.sentAt,
                        confirmationRequest.expiresAt,
                        confirmationRequest.responseDeadlineHours,
                        confirmationRequest.active,
                        customerResponse.responseType,
                        customerResponse.comment,
                        customerResponse.receivedAt
                )
                .from(confirmationRequest)
                .leftJoin(customerResponse)
                .on(
                        customerResponse.confirmationRequest.eq(
                                confirmationRequest
                        )
                )
                .where(
                        confirmationRequest.order.id.eq(orderId)
                )
                .orderBy(
                        confirmationRequest.sentAt.desc(),
                        confirmationRequest.id.desc()
                )
                .fetch();

        return rows.stream()
                .map(row -> toRequestDetail(
                        row,
                        confirmationRequest,
                        customerResponse
                ))
                .toList();
    }

    private RequestDetail toRequestDetail(
            Tuple row,
            QConfirmationRequest confirmationRequest,
            QCustomerResponse customerResponse
    ) {
        return new RequestDetail(
                row.get(confirmationRequest.id),
                row.get(confirmationRequest.communicationChannel),
                row.get(confirmationRequest.deliverySlot.date),
                row.get(confirmationRequest.deliverySlot.start),
                row.get(confirmationRequest.deliverySlot.end),
                row.get(confirmationRequest.sentAt),
                row.get(confirmationRequest.expiresAt),
                row.get(confirmationRequest.responseDeadlineHours),
                Boolean.TRUE.equals(
                        row.get(confirmationRequest.active)
                ),
                toCustomerResponseDetail(
                        row,
                        customerResponse
                )
        );
    }

    private CustomerResponseDetail toCustomerResponseDetail(
            Tuple row,
            QCustomerResponse customerResponse
    ) {
        CustomerResponseType responseType =
                row.get(customerResponse.responseType);

        if (responseType == null) {
            return null;
        }

        return new CustomerResponseDetail(
                responseType,
                row.get(customerResponse.comment),
                row.get(customerResponse.receivedAt)
        );
    }
}
