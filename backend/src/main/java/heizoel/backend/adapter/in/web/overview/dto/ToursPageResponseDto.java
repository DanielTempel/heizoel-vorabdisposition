package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.application.model.overview.TourOverviewPage;

import java.util.List;

public record ToursPageResponseDto(
        List<TourResponseDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ToursPageResponseDto from(
            TourOverviewPage result
    ) {
        return new ToursPageResponseDto(
                result.items()
                        .stream()
                        .map(TourResponseDto::from)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}