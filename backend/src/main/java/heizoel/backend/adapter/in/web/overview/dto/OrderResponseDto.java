package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.application.model.overview.OrderOverviewItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record OrderResponseDto(
        String externalOrderId,
        String customerName,
        String deliveryAddress,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        ConfirmationStatus confirmationStatus,
        Instant expiresAt
) {

    public static OrderResponseDto from(
            OrderOverviewItem order
    ) {
        return new OrderResponseDto(
                order.externalOrderId(),
                order.customerName(),
                order.deliveryAddress(),
                order.deliveryWindowStart(),
                order.deliveryWindowEnd(),
                order.communicationChannel(),
                order.confirmationStatus(),
                order.expiresAt()
        );
    }
}