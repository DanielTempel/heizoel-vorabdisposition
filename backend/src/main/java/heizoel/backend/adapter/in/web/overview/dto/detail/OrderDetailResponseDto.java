package heizoel.backend.adapter.in.web.overview.dto.detail;

import heizoel.backend.application.model.overview.ConfirmationDetail.OrderDetail;
import heizoel.backend.domain.ConfirmationStatus;

public record OrderDetailResponseDto(
        String externalOrderId,
        String customerName,
        String customerEmail,
        String customerPhoneNumber,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        String priceDisplayText,
        String tourNumber,
        String vehicleLicensePlate,
        ConfirmationStatus confirmationStatus
) {

    public static OrderDetailResponseDto from(
            OrderDetail order
    ) {
        return new OrderDetailResponseDto(
                order.externalOrderId(),
                order.customerName(),
                order.customerEmail(),
                order.customerPhoneNumber(),
                order.deliveryAddress(),
                order.product(),
                order.quantityLiters(),
                order.priceDisplayText(),
                order.tourNumber(),
                order.vehicleLicensePlate(),
                order.confirmationStatus()
        );
    }
}