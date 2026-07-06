package heizoel.backend.dashboard.adapter.web.dto;

import heizoel.backend.dashboard.application.port.in.orders.DashboardOrdersPageResult;

import java.util.List;

public record DashboardOrdersPageResponseDto(
        List<DashboardOrderResponseDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static DashboardOrdersPageResponseDto from(DashboardOrdersPageResult result) {
        return new DashboardOrdersPageResponseDto(
                result.items()
                        .stream()
                        .map(DashboardOrderResponseDto::from)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}