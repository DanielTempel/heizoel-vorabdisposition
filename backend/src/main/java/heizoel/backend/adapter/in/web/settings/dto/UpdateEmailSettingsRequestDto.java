package heizoel.backend.adapter.in.web.settings.dto;

import heizoel.backend.domain.company.SmtpSecurityMode;
import jakarta.validation.constraints.*;

public record UpdateEmailSettingsRequestDto(

        @NotBlank(message = "SMTP host must not be blank.")
        @Size(max = 255, message = "SMTP host must not exceed 255 characters.")
        String smtpHost,

        @NotNull(message = "SMTP port is required.")
        @Min(value = 1, message = "SMTP port must be at least 1.")
        @Max(value = 65535, message = "SMTP port must not exceed 65535.")
        Integer smtpPort,

        @NotNull(message = "SMTP security mode is required.")
        SmtpSecurityMode securityMode,

        boolean authenticationEnabled,

        @Size(max = 320, message = "SMTP username must not exceed 320 characters.")
        String username,

        @Size(max = 1000, message = "SMTP password must not exceed 1000 characters.")
        String password,

        @NotBlank(message = "Sender address must not be blank.")
        @Email(message = "Sender address must be a valid e-mail address.")
        @Size(max = 320, message = "Sender address must not exceed 320 characters.")
        String fromAddress,

        @NotBlank(message = "Sender name must not be blank.")
        @Size(max = 200, message = "Sender name must not exceed 200 characters.")
        String fromName
) {
}