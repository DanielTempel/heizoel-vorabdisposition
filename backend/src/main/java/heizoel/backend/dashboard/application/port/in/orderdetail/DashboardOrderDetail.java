package heizoel.backend.dashboard.application.port.in.orderdetail;

import heizoel.backend.domain.model.enumeration.ConfirmationStatus;

public record DashboardOrderDetail(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        String priceDisplayText,
        ConfirmationStatus confirmationStatus,
        DashboardLatestRequest latestRequest,
        DashboardLatestCustomerResponse latestCustomerResponse
) {
}