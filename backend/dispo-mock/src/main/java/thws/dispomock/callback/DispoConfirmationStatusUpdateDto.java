package thws.dispomock.callback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record DispoConfirmationStatusUpdateDto(
        @NotBlank String externalOrderId,
        @NotNull ConfirmationStatus confirmationStatus,
        String customerComment
) {
}
