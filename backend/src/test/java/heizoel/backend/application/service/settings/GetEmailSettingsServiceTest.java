package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.GetEmailSettingsResult;
import heizoel.backend.domain.company.CompanyEmailSettings;
import heizoel.backend.domain.company.SmtpSecurityMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetEmailSettingsServiceTest {

    private CompanyEmailSettingsRepository repository;
    private GetEmailSettingsService service;

    @BeforeEach
    void setUp() {
        repository = mock(
                CompanyEmailSettingsRepository.class
        );

        service = new GetEmailSettingsService(repository);
    }

    @Test
    void returnsSettingsForCompany() {
        CompanyEmailSettings settings =
                mock(CompanyEmailSettings.class);

        Instant updatedAt = Instant.parse("2026-08-05T20:03:00Z");

        when(repository.findByCompanyId(1L)).thenReturn(Optional.of(settings));

        when(settings.getSmtpHost()).thenReturn("smtp.example.de");
        when(settings.getSmtpPort()).thenReturn(587);
        when(settings.getSecurityMode()).thenReturn(SmtpSecurityMode.STARTTLS);
        when(settings.isAuthenticationEnabled()).thenReturn(true);
        when(settings.getUsername()).thenReturn("dispo@example.de");
        when(settings.hasConfiguredPassword()).thenReturn(true);
        when(settings.getFromAddress()).thenReturn("dispo@example.de");
        when(settings.getFromName()).thenReturn("Example Heizöl");
        when(settings.getUpdatedAt()).thenReturn(updatedAt);

        GetEmailSettingsResult result =
                service.getEmailSettings(
                        new CompanyContext(1L)
                );

        assertThat(result.configured()).isTrue();
        assertThat(result.smtpHost()).isEqualTo("smtp.example.de");
        assertThat(result.smtpPort()).isEqualTo(587);
        assertThat(result.securityMode()).isEqualTo(SmtpSecurityMode.STARTTLS);
        assertThat(result.authenticationEnabled()).isTrue();
        assertThat(result.username()).isEqualTo("dispo@example.de");
        assertThat(result.passwordConfigured()).isTrue();
        assertThat(result.fromAddress()).isEqualTo("dispo@example.de");
        assertThat(result.fromName()).isEqualTo("Example Heizöl");
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void returnsNotConfiguredWhenSettingsDoNotExist() {
        when(repository.findByCompanyId(1L)).thenReturn(Optional.empty());

        GetEmailSettingsResult result =
                service.getEmailSettings(
                        new CompanyContext(1L)
                );

        assertThat(result.configured()).isFalse();
        assertThat(result.smtpHost()).isNull();
        assertThat(result.smtpPort()).isNull();
        assertThat(result.securityMode()).isNull();
        assertThat(result.authenticationEnabled()).isFalse();
        assertThat(result.username()).isNull();
        assertThat(result.passwordConfigured()).isFalse();
        assertThat(result.fromAddress()).isNull();
        assertThat(result.fromName()).isNull();
        assertThat(result.updatedAt()).isNull();
    }
}