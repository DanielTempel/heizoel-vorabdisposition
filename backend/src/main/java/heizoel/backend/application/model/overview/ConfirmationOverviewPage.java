package heizoel.backend.application.model.overview;

import java.util.List;

public record ConfirmationOverviewPage(
        List<ConfirmationOverviewItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
