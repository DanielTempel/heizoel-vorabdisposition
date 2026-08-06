package heizoel.backend.application.port.in.settings;

import heizoel.backend.application.context.CompanyContext;

public interface SendTestEmailUseCase {

    void sendTestEmail(CompanyContext companyContext);
}
