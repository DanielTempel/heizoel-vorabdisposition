package heizoel.backend.dashboard.application.port.in.orders;

import java.util.List;

public record DashboardOrdersPageResult(
        List<DashboardOrderRaw> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
