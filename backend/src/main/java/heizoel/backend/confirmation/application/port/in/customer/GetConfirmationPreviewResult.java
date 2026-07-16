package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.domain.model.enumeration.ConfirmationStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record GetConfirmationPreviewResult(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        String priceDisplayText,
        ConfirmationStatus confirmationStatus
) {
}
