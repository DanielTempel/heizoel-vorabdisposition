package heizoel.backend.customer.api.dto;

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
        LocalTime deliveryWindowEnd
) {
}
