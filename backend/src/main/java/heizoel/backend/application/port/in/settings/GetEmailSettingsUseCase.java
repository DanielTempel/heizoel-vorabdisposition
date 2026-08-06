package heizoel.backend.application.port.in.settings;

import heizoel.backend.application.context.CompanyContext;

public interface GetEmailSettingsUseCase {

    GetEmailSettingsResult getEmailSettings(CompanyContext companyContext);
}
