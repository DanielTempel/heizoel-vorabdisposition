package heizoel.backend.application.model.overview;

import java.util.List;

public record TourOverviewPage(
        List<TourOverviewItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public TourOverviewPage {
        items = List.copyOf(items);
    }
}
