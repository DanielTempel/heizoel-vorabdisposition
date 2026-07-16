package heizoel.backend.dashboard.application.usecase;

import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.domain.exception.OrderSnapshotNotFoundException;
import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.CustomerResponse;
import heizoel.backend.domain.model.OrderSnapshot;
import heizoel.backend.dashboard.application.port.in.orderdetail.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDashboardOrderDetailUseCaseImpl implements GetDashboardOrderDetailUseCase {

    private final OrderSnapshotRepositoryPort orderSnapshotRepository;
    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final CustomerResponseRepositoryPort customerResponseRepository;

    @Override
    public DashboardOrderDetail getOrderDetail(GetDashboardOrderDetailQuery query) {
        OrderSnapshot orderSnapshot = orderSnapshotRepository
                .findByCompanyIdAndExternalOrderId(
                        query.companyContext().companyId(),
                        query.externalOrderId()
                )
                .orElseThrow(() -> new OrderSnapshotNotFoundException(
                        "Order snapshot was not found."
                ));

        ConfirmationRequest latestRequest = confirmationRequestRepository
                .findLatestByOrderSnapshot(orderSnapshot)
                .orElse(null);

        DashboardLatestRequest latestRequestResult = latestRequest != null
                ? toLatestRequest(latestRequest)
                : null;

        DashboardLatestCustomerResponse latestCustomerResponseResult = latestRequest != null
                ? findLatestCustomerResponse(latestRequest)
                : null;

        return new DashboardOrderDetail(
                orderSnapshot.getExternalOrderId(),
                orderSnapshot.getCustomerName(),
                orderSnapshot.getDeliveryAddress(),
                orderSnapshot.getProduct(),
                orderSnapshot.getQuantityLiters(),
                orderSnapshot.getPriceDisplayText(),
                orderSnapshot.getConfirmationStatus(),
                latestRequestResult,
                latestCustomerResponseResult
        );
    }

    private DashboardLatestCustomerResponse findLatestCustomerResponse(
            ConfirmationRequest latestRequest
    ) {
        return customerResponseRepository
                .findByConfirmationRequest(latestRequest)
                .map(this::toLatestCustomerResponse)
                .orElse(null);
    }

    private DashboardLatestRequest toLatestRequest(ConfirmationRequest request) {
        return new DashboardLatestRequest(
                request.getDeliveryDate(),
                request.getDeliveryWindowStart(),
                request.getDeliveryWindowEnd(),
                request.getCommunicationChannel(),
                request.getSentAt(),
                request.getExpiresAt(),
                request.isActive()
        );
    }

    private DashboardLatestCustomerResponse toLatestCustomerResponse(CustomerResponse response) {
        return new DashboardLatestCustomerResponse(
                response.getResponseType(),
                response.getComment(),
                response.getReceivedAt()
        );
    }

}