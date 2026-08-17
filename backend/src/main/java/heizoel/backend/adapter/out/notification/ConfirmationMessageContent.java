package heizoel.backend.adapter.out.notification;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;

import java.time.format.DateTimeFormatter;

public record ConfirmationMessageContent(
        String customerName,
        String externalOrderId,
        String product,
        String quantityLiters,
        String deliveryDate,
        String deliveryWindow,
        String deliveryAddress,
        String confirmationLink
) {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static ConfirmationMessageContent from(
            Order order,
            ConfirmationRequest confirmationRequest,
            String frontendUrl
    ) {
        return new ConfirmationMessageContent(
                order.getCustomerName(),
                order.getExternalOrderId(),
                order.getProduct(),
                String.valueOf(order.getQuantityLiters()),
                confirmationRequest.getDeliverySlot().getDate().format(DATE_FORMAT),
                confirmationRequest.getDeliverySlot().getStart().format(TIME_FORMAT)
                        + " - "
                        + confirmationRequest.getDeliverySlot().getEnd().format(TIME_FORMAT),
                order.getDeliveryAddress(),
                frontendUrl
                        + "/confirmation/"
                        + confirmationRequest.getToken()
        );
    }
}
