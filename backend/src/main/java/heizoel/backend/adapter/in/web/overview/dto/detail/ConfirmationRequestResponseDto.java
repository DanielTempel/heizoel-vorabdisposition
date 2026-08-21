package heizoel.backend.adapter.in.web.overview.dto.detail;

import heizoel.backend.application.model.overview.ConfirmationDetail.RequestDetail;
import heizoel.backend.domain.CommunicationChannel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmationRequestResponseDto(
        Long requestId,
        CommunicationChannel communicationChannel,
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        Instant sentAt,
        Instant expiresAt,
        Integer responseDeadlineHours,
        boolean active,
        String status,
        CustomerResponseResponseDto customerResponse
) {

    public static ConfirmationRequestResponseDto from(
            RequestDetail request
    ) {
        return new ConfirmationRequestResponseDto(
                request.requestId(),
                request.communicationChannel(),
                request.deliveryDate(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.sentAt(),
                request.expiresAt(),
                request.responseDeadlineHours(),
                request.active(),
                request.status(),
                request.customerResponse() != null
                        ? CustomerResponseResponseDto.from(
                        request.customerResponse()
                )
                        : null
        );
    }
}
