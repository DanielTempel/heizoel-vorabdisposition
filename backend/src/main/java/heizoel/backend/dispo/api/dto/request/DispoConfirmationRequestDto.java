package heizoel.backend.dispo.api.dto.request;

import heizoel.backend.notification.domain.CommunicationChannel;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record DispoConfirmationRequestDto(

        @NotBlank(message = "External order id must not be blank.")
        String externalOrderId,

        @NotBlank(message = "Customer name must not be blank.")
        String customerName,

        @NotNull(message = "Communication channel is required.")
        CommunicationChannel communicationChannel,

        @Email(message = "Customer e-mail must be a valid e-mail address.")
        String customerEmail,

        String customerPhoneNumber,

        @NotBlank(message = "Delivery address must not be blank.")
        String deliveryAddress,

        @NotBlank(message = "Product must not be blank.")
        String product,

        @NotNull(message = "Quantity in liters is required.")
        @Positive(message = "Quantity in liters must be greater than 0.")
        Integer quantityLiters,

        @NotNull(message = "Delivery date is required.")
        LocalDate deliveryDate,

        @NotNull(message = "Delivery window start is required.")
        LocalTime deliveryWindowStart,

        @NotNull(message = "Delivery window end is required.")
        LocalTime deliveryWindowEnd,

        @NotNull(message = "Response deadline in hours is required.")
        @Positive(message = "Response deadline in hours must be greater than 0.")
        @Max(value = 168, message = "Response deadline must not exceed 168 hours.")
        Integer responseDeadlineHours,

        @Size(max = 100, message = "Price display text must not exceed 100 characters.")
        String priceDisplayText
) {
}
