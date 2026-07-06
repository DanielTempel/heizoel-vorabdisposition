package heizoel.backend.dashboard.application.port.in;

import java.util.List;

public record DashboardOrdersPageResult(
        List<DashboardOrder> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
