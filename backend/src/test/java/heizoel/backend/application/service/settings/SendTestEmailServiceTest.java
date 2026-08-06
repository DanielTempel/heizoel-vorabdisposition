package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.notification.email.CompanyMailSenderFactory;
import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.TestEmailDeliveryException;
import heizoel.backend.domain.company.CompanyEmailSettings;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SendTestEmailServiceTest {

    private CompanyEmailSettingsRepository repository;
    private CompanyMailSenderFactory factory;
    private JavaMailSenderImpl mailSender;

    private SendTestEmailService service;

    @BeforeEach
    void setUp() {
        repository =
                mock(CompanyEmailSettingsRepository.class);

        factory =
                mock(CompanyMailSenderFactory.class);

        mailSender =
                mock(JavaMailSenderImpl.class);

        service = new SendTestEmailService(
                repository,
                factory
        );
    }

    @Test
    void sendsTestEmailToConfiguredFromAddress()
            throws Exception {

        CompanyEmailSettings settings =
                mock(CompanyEmailSettings.class);

        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(repository.findByCompanyId(1L))
                .thenReturn(Optional.of(settings));

        when(factory.create(1L, settings))
                .thenReturn(mailSender);

        when(mailSender.createMimeMessage())
                .thenReturn(message);

        when(settings.getFromAddress())
                .thenReturn("dispo@example.de");

        when(settings.getFromName())
                .thenReturn("Example Heizöl");

        service.sendTestEmail(
                new CompanyContext(1L)
        );

        verify(mailSender).send(message);

        InternetAddress from =
                (InternetAddress) message.getFrom()[0];

        InternetAddress recipient =
                (InternetAddress) message
                        .getRecipients(
                                Message.RecipientType.TO
                        )[0];

        assertThat(from.getAddress())
                .isEqualTo("dispo@example.de");

        assertThat(from.getPersonal())
                .isEqualTo("Example Heizöl");

        assertThat(recipient.getAddress())
                .isEqualTo("dispo@example.de");

        assertThat(message.getSubject())
                .isEqualTo(
                        "SMTP-Konfiguration erfolgreich getestet"
                );
    }

    @Test
    void wrapsTestEmailDeliveryFailure() {
        CompanyEmailSettings settings =
                mock(CompanyEmailSettings.class);

        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(repository.findByCompanyId(1L))
                .thenReturn(Optional.of(settings));

        when(factory.create(1L, settings))
                .thenReturn(mailSender);

        when(mailSender.createMimeMessage())
                .thenReturn(message);

        when(settings.getFromAddress())
                .thenReturn("dispo@example.de");

        when(settings.getFromName())
                .thenReturn("Example Heizöl");

        doThrow(new MailSendException("SMTP rejected message"))
                .when(mailSender)
                .send(message);

        assertThatThrownBy(() ->
                service.sendTestEmail(
                        new CompanyContext(1L)
                )
        )
                .isInstanceOf(
                        TestEmailDeliveryException.class
                )
                .hasMessage(
                        "Test e-mail could not be delivered."
                );
    }

}