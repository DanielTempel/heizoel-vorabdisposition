package heizoel.backend.adapter.in.web.overview.dto;


import heizoel.backend.application.model.overview.OrderOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.domain.ConfirmationStatus;

import java.time.LocalDate;
import java.util.List;

public record TourResponseDto(
        String tourNumber,
        String vehicleLicensePlate,
        LocalDate deliveryDate,
        StatusCounts statusCounts,
        List<OrderResponseDto> orders
) {

    public static TourResponseDto from(TourOverviewItem tour) {
        return new TourResponseDto(
                tour.tourNumber(),
                tour.vehicleLicensePlate(),
                tour.deliveryDate(),
                StatusCounts.from(tour.orders()),
                tour.orders()
                        .stream()
                        .map(OrderResponseDto::from)
                        .toList()
        );
    }

    public record StatusCounts(
            int sent,
            int confirmed,
            int rejected,
            int noResponse
    ) {

        private static StatusCounts from(
                List<OrderOverviewItem> orders
        ) {
            int sent = 0;
            int confirmed = 0;
            int rejected = 0;
            int noResponse = 0;

            for (OrderOverviewItem order : orders) {
                ConfirmationStatus status =
                        order.confirmationStatus();

                switch (status) {
                    case OPEN -> { /* not use it yet */ }
                    case SENT -> sent++;
                    case CONFIRMED -> confirmed++;
                    case REJECTED -> rejected++;
                    case NO_RESPONSE -> noResponse++;
                }
            }

            return new StatusCounts(
                    sent,
                    confirmed,
                    rejected,
                    noResponse
            );
        }
    }


}