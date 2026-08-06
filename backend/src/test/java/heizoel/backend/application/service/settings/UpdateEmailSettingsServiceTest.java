package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.InvalidEmailSettingsException;
import heizoel.backend.application.port.in.settings.UpdateEmailSettingsCommand;
import heizoel.backend.application.port.out.security.SecretEncryptionService;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.company.CompanyEmailSettings;
import heizoel.backend.domain.company.SmtpSecurityMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UpdateEmailSettingsServiceTest {

    private CompanyRepository companyRepository;
    private CompanyEmailSettingsRepository settingsRepository;
    private SecretEncryptionService secretEncryptionService;

    private UpdateEmailSettingsService service;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        settingsRepository =
                mock(CompanyEmailSettingsRepository.class);
        secretEncryptionService =
                mock(SecretEncryptionService.class);

        service = new UpdateEmailSettingsService(
                companyRepository,
                settingsRepository,
                secretEncryptionService
        );
    }

    @Test
    void createsSettingsAndEncryptsPassword() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.empty());

        when(secretEncryptionService.encrypt(
                "smtp-secret",
                "company-email-settings:1"
        )).thenReturn("encrypted-secret");

        service.updateEmailSettings(command(
                true,
                "smtp-user",
                "smtp-secret"
        ));

        ArgumentCaptor<CompanyEmailSettings> captor =
                ArgumentCaptor.forClass(
                        CompanyEmailSettings.class
                );

        verify(settingsRepository).save(captor.capture());

        CompanyEmailSettings saved = captor.getValue();

        assertThat(saved.getSmtpHost())
                .isEqualTo("smtp.example.de");
        assertThat(saved.getSmtpPort()).isEqualTo(587);
        assertThat(saved.getSecurityMode())
                .isEqualTo(SmtpSecurityMode.STARTTLS);
        assertThat(saved.isAuthenticationEnabled()).isTrue();
        assertThat(saved.getUsername())
                .isEqualTo("smtp-user");
        assertThat(saved.getEncryptedPassword())
                .isEqualTo("encrypted-secret");
    }

    @Test
    void keepsExistingPasswordWhenPasswordIsNotProvided() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        CompanyEmailSettings existing =
                CompanyEmailSettings.create(
                        company,
                        "old-smtp.example.de",
                        587,
                        SmtpSecurityMode.STARTTLS,
                        true,
                        "old-user",
                        "existing-encrypted-password",
                        "old@example.de",
                        "Old Sender"
                );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.of(existing));

        service.updateEmailSettings(command(
                true,
                "new-user",
                null
        ));

        assertThat(existing.getUsername())
                .isEqualTo("new-user");

        assertThat(existing.getEncryptedPassword())
                .isEqualTo("existing-encrypted-password");

        verifyNoInteractions(secretEncryptionService);
        verify(settingsRepository).save(existing);
    }

    @Test
    void encryptsAndReplacesNewPassword() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        CompanyEmailSettings existing =
                CompanyEmailSettings.create(
                        company,
                        "old-smtp.example.de",
                        587,
                        SmtpSecurityMode.STARTTLS,
                        true,
                        "old-user",
                        "old-encrypted-password",
                        "old@example.de",
                        "Old Sender"
                );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.of(existing));

        when(secretEncryptionService.encrypt(
                "new-password",
                "company-email-settings:1"
        )).thenReturn("new-encrypted-password");

        service.updateEmailSettings(command(
                true,
                "new-user",
                "new-password"
        ));

        assertThat(existing.getEncryptedPassword())
                .isEqualTo("new-encrypted-password");
    }

    @Test
    void clearsCredentialsWhenAuthenticationIsDisabled() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        CompanyEmailSettings existing =
                CompanyEmailSettings.create(
                        company,
                        "smtp.example.de",
                        587,
                        SmtpSecurityMode.STARTTLS,
                        true,
                        "smtp-user",
                        "encrypted-password",
                        "sender@example.de",
                        "Sender"
                );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.of(existing));

        service.updateEmailSettings(command(
                false,
                "ignored-user",
                "ignored-password"
        ));

        assertThat(existing.isAuthenticationEnabled()).isFalse();
        assertThat(existing.getUsername()).isNull();
        assertThat(existing.getEncryptedPassword()).isNull();

        verifyNoInteractions(secretEncryptionService);
    }

    @Test
    void rejectsEnabledAuthenticationWithoutAvailablePassword() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateEmailSettings(command(
                        true,
                        "smtp-user",
                        null
                ))
        )
                .isInstanceOf(
                        InvalidEmailSettingsException.class
                )
                .hasMessage(
                        "SMTP password is required when authentication is enabled."
                );

        verify(settingsRepository, never()).save(any());
    }

    @Test
    void rejectsEnabledAuthenticationWithoutUsername() {
        Company company = Company.create(
                "Example",
                "api-key-hash",
                "http://localhost/callback"
        );

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        when(settingsRepository.findByCompanyId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateEmailSettings(command(
                        true,
                        null,
                        "smtp-secret"
                ))
        )
                .isInstanceOf(
                        InvalidEmailSettingsException.class
                )
                .hasMessage(
                        "SMTP username is required when authentication is enabled."
                );
    }

    private UpdateEmailSettingsCommand command(
            boolean authenticationEnabled,
            String username,
            String password
    ) {
        return new UpdateEmailSettingsCommand(
                new CompanyContext(1L),
                "smtp.example.de",
                587,
                SmtpSecurityMode.STARTTLS,
                authenticationEnabled,
                username,
                password,
                "sender@example.de",
                "Example Sender"
        );
    }
}