package heizoel.backend.confirmation.adapter.web.customer.dto;

import heizoel.backend.domain.model.enumeration.CustomerResponseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerResponseRequestDto(
        @NotNull(message = "Response type is required.")
        CustomerResponseType responseType,

        @Size(max = 2000, message = "Customer comment must not exceed 2000 characters.")
        String customerComment
) {
}
