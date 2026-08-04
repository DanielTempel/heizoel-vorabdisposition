package heizoel.backend.application.model.overview;

import java.time.LocalDate;
import java.util.List;

public record TourOverviewItem(
        String tourNumber,
        String vehicleLicensePlate,
        LocalDate deliveryDate,
        List<OrderOverviewItem> orders
) {
    public TourOverviewItem {
        orders = List.copyOf(orders);
    }
}