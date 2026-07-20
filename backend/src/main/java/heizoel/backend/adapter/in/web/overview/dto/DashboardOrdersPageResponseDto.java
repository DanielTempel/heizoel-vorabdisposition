package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.application.model.overview.ConfirmationOverviewPage;

import java.util.List;

public record DashboardOrdersPageResponseDto(
        List<DashboardOrderResponseDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static DashboardOrdersPageResponseDto from(ConfirmationOverviewPage result) {
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