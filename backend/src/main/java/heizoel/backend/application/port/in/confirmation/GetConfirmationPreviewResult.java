package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.domain.ConfirmationStatus;

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
