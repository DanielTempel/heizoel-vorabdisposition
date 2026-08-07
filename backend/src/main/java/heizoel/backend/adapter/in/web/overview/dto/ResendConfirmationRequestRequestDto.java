package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.domain.CommunicationChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResendConfirmationRequestRequestDto(

        @NotNull(message = "Communication channel is required.")
        CommunicationChannel communicationChannel,

        @NotNull(message = "Response deadline in hours is required.")
        @Positive(message = "Response deadline in hours must be greater than 0.")
        @Max(
                value = 168,
                message = "Response deadline must not exceed 168 hours."
        )
        Integer responseDeadlineHours
) {
}