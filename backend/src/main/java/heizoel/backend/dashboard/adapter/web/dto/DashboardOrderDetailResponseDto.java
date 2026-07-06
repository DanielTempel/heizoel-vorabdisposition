package heizoel.backend.dashboard.adapter.web.dto;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.dashboard.application.port.in.orderdetail.DashboardOrderDetail;

public record DashboardOrderDetailResponseDto(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        String priceDisplayText,
        ConfirmationStatus confirmationStatus,
        DashboardLatestRequestResponseDto latestRequest,
        DashboardLatestCustomerResponseDto latestCustomerResponse
) {
    public static DashboardOrderDetailResponseDto from(DashboardOrderDetail detail) {
        return new DashboardOrderDetailResponseDto(
                detail.externalOrderId(),
                detail.customerName(),
                detail.deliveryAddress(),
                detail.product(),
                detail.quantityLiters(),
                detail.priceDisplayText(),
                detail.confirmationStatus(),
                detail.latestRequest() != null
                        ? DashboardLatestRequestResponseDto.from(detail.latestRequest())
                        : null,
                detail.latestCustomerResponse() != null
                        ? DashboardLatestCustomerResponseDto.from(detail.latestCustomerResponse())
                        : null
        );
    }
}