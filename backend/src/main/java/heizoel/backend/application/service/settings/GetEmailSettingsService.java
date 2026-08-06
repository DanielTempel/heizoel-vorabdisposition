package heizoel.backend.application.service.settings;


import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.GetEmailSettingsResult;
import heizoel.backend.application.port.in.settings.GetEmailSettingsUseCase;
import heizoel.backend.domain.company.CompanyEmailSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEmailSettingsService implements GetEmailSettingsUseCase {

    private final CompanyEmailSettingsRepository companyEmailSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public GetEmailSettingsResult getEmailSettings(
            CompanyContext companyContext
    ) {
        return companyEmailSettingsRepository
                .findByCompanyId(companyContext.companyId())
                .map(this::toResult)
                .orElseGet(GetEmailSettingsResult::notConfigured);
    }

    private GetEmailSettingsResult toResult(
            CompanyEmailSettings settings
    ) {
        return new GetEmailSettingsResult(
                true,
                settings.getSmtpHost(),
                settings.getSmtpPort(),
                settings.getSecurityMode(),
                settings.isAuthenticationEnabled(),
                settings.getUsername(),
                settings.hasConfiguredPassword(),
                settings.getFromAddress(),
                settings.getFromName(),
                settings.getUpdatedAt()
        );
    }
}
