package heizoel.backend.confirmation.adapter.web.customer.dto;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record CustomerConfirmationPreviewDto(
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

