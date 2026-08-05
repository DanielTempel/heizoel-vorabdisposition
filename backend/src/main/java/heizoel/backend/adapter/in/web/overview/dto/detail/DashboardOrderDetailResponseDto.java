package heizoel.backend.adapter.in.web.overview.dto.detail;

import heizoel.backend.application.model.overview.ConfirmationDetail;

import java.util.List;

public record DashboardOrderDetailResponseDto(
        OrderDetailResponseDto order,
        ConfirmationRequestResponseDto currentRequest,
        List<ConfirmationRequestResponseDto> previousRequests
) {

    public DashboardOrderDetailResponseDto {
        previousRequests = List.copyOf(previousRequests);
    }

    public static DashboardOrderDetailResponseDto from(
            ConfirmationDetail detail
    ) {
        return new DashboardOrderDetailResponseDto(
                OrderDetailResponseDto.from(detail.order()),
                detail.currentRequest() != null
                        ? ConfirmationRequestResponseDto.from(
                        detail.currentRequest()
                )
                        : null,
                detail.previousRequests()
                        .stream()
                        .map(ConfirmationRequestResponseDto::from)
                        .toList()
        );
    }
}