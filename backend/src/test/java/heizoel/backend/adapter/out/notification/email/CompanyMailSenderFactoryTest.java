package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.application.port.out.security.SecretEncryptionService;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.company.CompanyEmailSettings;
import heizoel.backend.domain.company.SmtpSecurityMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CompanyMailSenderFactoryTest {

    private SecretEncryptionService secretEncryptionService;
    private CompanyMailSenderFactory factory;

    @BeforeEach
    void setUp() {
        secretEncryptionService = mock(SecretEncryptionService.class);

        factory = new CompanyMailSenderFactory(
                secretEncryptionService
        );
    }

    @Test
    void createsAuthenticatedStartTlsSender() {
        CompanyEmailSettings settings = createSettings(
                SmtpSecurityMode.STARTTLS
        );

        when(secretEncryptionService.decrypt(
                "encrypted-password",
                "company-email-settings:1"
        )).thenReturn("plain-password");

        JavaMailSenderImpl sender = factory.create(
                1L,
                settings
        );

        Properties properties =
                sender.getJavaMailProperties();

        assertThat(sender.getHost()).isEqualTo("smtp.example.de");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getUsername()).isEqualTo("smtp-user");
        assertThat(sender.getPassword()).isEqualTo("plain-password");

        assertThat(properties.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(properties.getProperty(
                "mail.smtp.starttls.enable"
        )).isEqualTo("true");
        assertThat(properties.getProperty(
                "mail.smtp.starttls.required"
        )).isEqualTo("true");
        assertThat(properties.getProperty(
                "mail.smtp.ssl.enable"
        )).isEqualTo("false");
    }

    @Test
    void createsImplicitTlsSender() {
        CompanyEmailSettings settings = createSettings(
                SmtpSecurityMode.IMPLICIT_TLS
        );

        when(secretEncryptionService.decrypt(
                anyString(),
                anyString()
        )).thenReturn("plain-password");

        JavaMailSenderImpl sender = factory.create(
                1L,
                settings
        );

        Properties properties =
                sender.getJavaMailProperties();

        assertThat(properties.getProperty(
                "mail.smtp.starttls.enable"
        )).isEqualTo("false");
        assertThat(properties.getProperty(
                "mail.smtp.ssl.enable"
        )).isEqualTo("true");
        assertThat(properties.getProperty(
                "mail.smtp.ssl.checkserveridentity"
        )).isEqualTo("true");
    }

    @Test
    void doesNotDecryptPasswordWhenAuthenticationIsDisabled() {
        CompanyEmailSettings settings =
                CompanyEmailSettings.create(
                        Company.create(
                                "Example",
                                "api-key-hash",
                                "http://localhost/callback"
                        ),
                        "localhost",
                        1025,
                        SmtpSecurityMode.NONE,
                        false,
                        null,
                        null,
                        "sender@example.de",
                        "Example Sender"
                );

        JavaMailSenderImpl sender = factory.create(
                1L,
                settings
        );

        assertThat(sender.getUsername()).isNull();
        assertThat(sender.getPassword()).isNull();

        assertThat(sender.getJavaMailProperties()
                .getProperty("mail.smtp.auth"))
                .isEqualTo("false");

        verifyNoInteractions(secretEncryptionService);
    }

    private CompanyEmailSettings createSettings(
            SmtpSecurityMode securityMode
    ) {
        return CompanyEmailSettings.create(
                Company.create(
                        "Example",
                        "api-key-hash",
                        "http://localhost/callback"
                ),
                "smtp.example.de",
                587,
                securityMode,
                true,
                "smtp-user",
                "encrypted-password",
                "sender@example.de",
                "Example Sender"
        );
    }
}