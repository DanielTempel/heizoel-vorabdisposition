package heizoel.backend.application.service.settings;


import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.application.exception.CompanyNotFoundException;
import heizoel.backend.application.exception.InvalidEmailSettingsException;
import heizoel.backend.application.port.in.settings.UpdateEmailSettingsCommand;
import heizoel.backend.application.port.in.settings.UpdateEmailSettingsUseCase;
import heizoel.backend.application.port.out.security.SecretEncryptionService;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.company.CompanyEmailSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEmailSettingsService implements UpdateEmailSettingsUseCase {

    private static final String ENCRYPTION_CONTEXT_PREFIX = "company-email-settings:";

    private final CompanyRepository companyRepository;
    private final CompanyEmailSettingsRepository companyEmailSettingsRepository;
    private final SecretEncryptionService secretEncryptionService;

    @Override
    @Transactional
    public void updateEmailSettings(
            UpdateEmailSettingsCommand command
    ) {
        Long companyId = command.companyContext().companyId();

        Company company = companyRepository
                .findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(
                        "Company was not found."
                ));

        CompanyEmailSettings existingSettings =
                companyEmailSettingsRepository
                        .findByCompanyId(companyId)
                        .orElse(null);

        validateAuthentication(
                command,
                existingSettings
        );

        String encryptedPassword = resolveEncryptedPassword(
                command,
                existingSettings,
                companyId
        );

        if (existingSettings == null) {
            CompanyEmailSettings newSettings =
                    CompanyEmailSettings.create(
                            company,
                            command.smtpHost().trim(),
                            command.smtpPort(),
                            command.securityMode(),
                            command.authenticationEnabled(),
                            normalizeUsername(command),
                            encryptedPassword,
                            command.fromAddress().trim(),
                            command.fromName().trim()
                    );

            companyEmailSettingsRepository.save(newSettings);
            return;
        }

        existingSettings.update(
                command.smtpHost().trim(),
                command.smtpPort(),
                command.securityMode(),
                command.authenticationEnabled(),
                normalizeUsername(command),
                encryptedPassword,
                command.fromAddress().trim(),
                command.fromName().trim()
        );

        companyEmailSettingsRepository.save(existingSettings);
    }

    private void validateAuthentication(
            UpdateEmailSettingsCommand command,
            CompanyEmailSettings existingSettings
    ) {
        if (!command.authenticationEnabled()) {
            return;
        }

        if (!hasText(command.username())) {
            throw new InvalidEmailSettingsException(
                    "SMTP username is required when authentication is enabled."
            );
        }

        boolean passwordAvailable =
                hasText(command.password())
                        || existingSettings != null
                        && existingSettings.hasConfiguredPassword();

        if (!passwordAvailable) {
            throw new InvalidEmailSettingsException(
                    "SMTP password is required when authentication is enabled."
            );
        }
    }

    private String resolveEncryptedPassword(
            UpdateEmailSettingsCommand command,
            CompanyEmailSettings existingSettings,
            Long companyId
    ) {
        if (!command.authenticationEnabled()) {
            return null;
        }

        if (hasText(command.password())) {
            return secretEncryptionService.encrypt(
                    command.password(),
                    encryptionContext(companyId)
            );
        }

        if (existingSettings == null || !existingSettings.hasConfiguredPassword()) {
            throw new InvalidEmailSettingsException(
                    "SMTP password is required when authentication is enabled."
            );
        }

        return existingSettings.getEncryptedPassword();
    }

    private String normalizeUsername(
            UpdateEmailSettingsCommand command
    ) {
        if (!command.authenticationEnabled()) {
            return null;
        }

        return command.username().trim();
    }

    private String encryptionContext(Long companyId) {
        return ENCRYPTION_CONTEXT_PREFIX + companyId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }



}
