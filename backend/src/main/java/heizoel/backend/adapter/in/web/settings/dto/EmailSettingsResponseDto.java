package heizoel.backend.adapter.in.web.settings.dto;


import heizoel.backend.application.port.in.settings.GetEmailSettingsResult;
import heizoel.backend.domain.company.SmtpSecurityMode;

import java.time.Instant;

public record EmailSettingsResponseDto(
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

    public static EmailSettingsResponseDto from(GetEmailSettingsResult result) {
        return new EmailSettingsResponseDto(
                result.configured(),
                result.smtpHost(),
                result.smtpPort(),
                result.securityMode(),
                result.authenticationEnabled(),
                result.username(),
                result.passwordConfigured(),
                result.fromAddress(),
                result.fromName(),
                result.updatedAt()
        );
    }
}