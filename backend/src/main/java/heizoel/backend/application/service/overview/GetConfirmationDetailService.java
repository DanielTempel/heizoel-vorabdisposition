package heizoel.backend.application.service.overview;

import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.model.overview.LatestConfirmationRequest;
import heizoel.backend.application.model.overview.LatestCustomerResponse;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.application.exception.OrderSnapshotNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetConfirmationDetailService implements GetConfirmationDetailUseCase {

    private final OrderSnapshotRepositoryPort orderSnapshotRepository;
    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final CustomerResponseRepositoryPort customerResponseRepository;

    @Override
    public ConfirmationDetail getOrderDetail(GetDashboardOrderDetailQuery query) {
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

        LatestConfirmationRequest latestRequestResult = latestRequest != null
                ? toLatestRequest(latestRequest)
                : null;

        LatestCustomerResponse latestCustomerResponseResult = latestRequest != null
                ? findLatestCustomerResponse(latestRequest)
                : null;

        return new ConfirmationDetail(
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

    private LatestCustomerResponse findLatestCustomerResponse(
            ConfirmationRequest latestRequest
    ) {
        return customerResponseRepository
                .findByConfirmationRequest(latestRequest)
                .map(this::toLatestCustomerResponse)
                .orElse(null);
    }

    private LatestConfirmationRequest toLatestRequest(ConfirmationRequest request) {
        return new LatestConfirmationRequest(
                request.getDeliveryDate(),
                request.getDeliveryWindowStart(),
                request.getDeliveryWindowEnd(),
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