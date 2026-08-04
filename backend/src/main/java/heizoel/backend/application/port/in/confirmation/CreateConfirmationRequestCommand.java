package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.CommunicationChannel;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateConfirmationRequestCommand(
        CompanyContext companyContext,
        String externalOrderId,
        String tourNumber,
        String vehicleLicensePlate,
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
