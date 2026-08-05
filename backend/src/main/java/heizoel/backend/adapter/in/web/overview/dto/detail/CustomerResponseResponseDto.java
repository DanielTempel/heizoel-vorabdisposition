package heizoel.backend.adapter.in.web.overview.dto.detail;

import heizoel.backend.application.model.overview.ConfirmationDetail.CustomerResponseDetail;
import heizoel.backend.domain.CustomerResponseType;

import java.time.Instant;

public record CustomerResponseResponseDto(
        CustomerResponseType responseType,
        String comment,
        Instant receivedAt
) {

    public static CustomerResponseResponseDto from(
            CustomerResponseDetail response
    ) {
        return new CustomerResponseResponseDto(
                response.responseType(),
                response.comment(),
                response.receivedAt()
        );
    }
}