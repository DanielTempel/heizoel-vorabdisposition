package heizoel.backend.confirmation.adapter.in.web.customer.dto;

import jakarta.validation.constraints.Size;

public record CustomerAnswerRequestDto(

        @Size(max = 2000, message = "Customer comment must not exceed 2000 characters.")
        String customerComment

) {
}
