package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.domain.model.CommunicationChannel;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateConfirmationRequestCommand(
        String externalOrderId,
        String customerName,
        CommunicationChannel communicationChannel,
        String customerEmail,
        String customerPhoneNumber,
        String deliveryAddress,
        String product,
        Integer quantityLiters,
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        Integer responseDeadlineHours,
        String priceDisplayText
) {
}
