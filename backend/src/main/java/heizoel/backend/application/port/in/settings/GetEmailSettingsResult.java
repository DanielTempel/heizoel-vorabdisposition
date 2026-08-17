package heizoel.backend.application.port.in.settings;

import heizoel.backend.domain.company.SmtpSecurityMode;

import java.time.Instant;

public record GetEmailSettingsResult(
        boolean configured,
        String smtpHost,
        Integer smtpPort,
        SmtpSecurityMode securityMode,
        boolean authenticationEnabled,
        String username,
        boolean passwordConfigured,
        String fromAddress,
        String fromName,
        Instant updatedAt
) {

    public static GetEmailSettingsResult notConfigured() {
        return new GetEmailSettingsResult(
                false,
                null,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                null
        );
    }
}