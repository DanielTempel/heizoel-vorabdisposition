package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.application.model.overview.ConfirmationDetail;

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
    public static DashboardOrderDetailResponseDto from(ConfirmationDetail detail) {
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