package heizoel.backend.application.model.overview;

import heizoel.backend.domain.ConfirmationStatus;

public record ConfirmationDetail(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        String priceDisplayText,
        ConfirmationStatus confirmationStatus,
        LatestConfirmationRequest latestRequest,
        LatestCustomerResponse latestCustomerResponse
) {
}