package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.notification.email.CompanyMailSenderFactory;
import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.EmailConnectionTestException;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.domain.company.CompanyEmailSettings;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TestEmailConnectionServiceTest {

    private CompanyEmailSettingsRepository repository;
    private CompanyMailSenderFactory factory;
    private JavaMailSenderImpl mailSender;

    private TestEmailConnectionService service;

    @BeforeEach
    void setUp() {
        repository =
                mock(CompanyEmailSettingsRepository.class);
        factory =
                mock(CompanyMailSenderFactory.class);
        mailSender =
                mock(JavaMailSenderImpl.class);

        service = new TestEmailConnectionService(
                repository,
                factory
        );
    }

    @Test
    void testsConnectionUsingCompanySettings()
            throws MessagingException {

        CompanyEmailSettings settings =
                mock(CompanyEmailSettings.class);

        when(repository.findByCompanyId(1L))
                .thenReturn(Optional.of(settings));

        when(factory.create(1L, settings))
                .thenReturn(mailSender);

        service.testEmailConnection(
                new CompanyContext(1L)
        );

        verify(mailSender).testConnection();
    }

    @Test
    void rejectsTestWhenSettingsAreNotConfigured() {
        when(repository.findByCompanyId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.testEmailConnection(
                        new CompanyContext(1L)
                )
        )
                .isInstanceOf(
                        EmailSettingsNotConfiguredException.class
                )
                .hasMessage(
                        "E-mail settings are not configured."
                );

        verifyNoInteractions(factory);
    }

    @Test
    void wrapsSmtpConnectionFailure()
            throws MessagingException {

        CompanyEmailSettings settings =
                mock(CompanyEmailSettings.class);

        when(repository.findByCompanyId(1L))
                .thenReturn(Optional.of(settings));

        when(factory.create(1L, settings))
                .thenReturn(mailSender);

        doThrow(new MessagingException(
                "Authentication failed"
        ))
                .when(mailSender)
                .testConnection();

        assertThatThrownBy(() ->
                service.testEmailConnection(
                        new CompanyContext(1L)
                )
        )
                .isInstanceOf(
                        EmailConnectionTestException.class
                )
                .hasMessage(
                        "SMTP connection could not be established."
                );
    }
}