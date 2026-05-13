package thws.smsmock.sms;

import jakarta.validation.constraints.NotBlank;

public record SmsSendRequestDto(

        @NotBlank(message = "Recipient phone number must not be blank.")
        String to,

        @NotBlank(message = "SMS text must not be blank.")
        String text
) {
}