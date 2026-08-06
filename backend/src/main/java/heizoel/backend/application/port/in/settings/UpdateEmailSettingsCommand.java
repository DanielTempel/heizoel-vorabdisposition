package heizoel.backend.application.port.in.settings;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.company.SmtpSecurityMode;

public record UpdateEmailSettingsCommand(
        CompanyContext companyContext,
        String smtpHost,
        Integer smtpPort,
        SmtpSecurityMode securityMode,
        boolean authenticationEnabled,
        String username,
        String password,
        String fromAddress,
        String fromName
) {
}