package heizoel.backend.adapter.out.notification.email;


import heizoel.backend.application.port.out.security.SecretEncryptionService;
import heizoel.backend.domain.company.CompanyEmailSettings;
import heizoel.backend.domain.company.SmtpSecurityMode;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@RequiredArgsConstructor
public class CompanyMailSenderFactory {

    private static final String ENCRYPTION_CONTEXT_PREFIX = "company-email-settings:";

    private static final String CONNECTION_TIMEOUT_MILLIS = "5000";
    private static final String READ_TIMEOUT_MILLIS = "5000";
    private static final String WRITE_TIMEOUT_MILLIS = "5000";

    private final SecretEncryptionService secretEncryptionService;

    public JavaMailSenderImpl create(
            Long companyId,
            CompanyEmailSettings settings
    ) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort());
        mailSender.setDefaultEncoding("UTF-8");

        if (settings.isAuthenticationEnabled()) {
            mailSender.setUsername(settings.getUsername());
            mailSender.setPassword(
                    secretEncryptionService.decrypt(
                            settings.getEncryptedPassword(),
                            ENCRYPTION_CONTEXT_PREFIX + companyId
                    )
            );
        }

        mailSender.setJavaMailProperties(
                createMailProperties(settings)
        );

        return mailSender;
    }

    private Properties createMailProperties(
            CompanyEmailSettings settings
    ) {
        Properties properties = new Properties();

        properties.setProperty(
                "mail.smtp.auth",
                Boolean.toString(settings.isAuthenticationEnabled())
        );

        properties.setProperty(
                "mail.smtp.from",
                settings.getFromAddress()
        );

        properties.setProperty(
                "mail.smtp.connectiontimeout",
                CONNECTION_TIMEOUT_MILLIS
        );
        properties.setProperty(
                "mail.smtp.timeout",
                READ_TIMEOUT_MILLIS
        );
        properties.setProperty(
                "mail.smtp.writetimeout",
                WRITE_TIMEOUT_MILLIS
        );

        applySecurityMode(
                properties,
                settings.getSecurityMode()
        );

        return properties;
    }

    private void applySecurityMode(
            Properties properties,
            SmtpSecurityMode securityMode
    ) {
        switch (securityMode) {
            case STARTTLS -> {
                properties.setProperty(
                        "mail.smtp.starttls.enable",
                        "true"
                );
                properties.setProperty(
                        "mail.smtp.starttls.required",
                        "true"
                );
                properties.setProperty(
                        "mail.smtp.ssl.enable",
                        "false"
                );
                properties.setProperty(
                        "mail.smtp.ssl.checkserveridentity",
                        "true"
                );
            }

            case IMPLICIT_TLS -> {
                properties.setProperty(
                        "mail.smtp.starttls.enable",
                        "false"
                );
                properties.setProperty(
                        "mail.smtp.ssl.enable",
                        "true"
                );
                properties.setProperty(
                        "mail.smtp.ssl.checkserveridentity",
                        "true"
                );
            }

            case NONE -> {
                properties.setProperty(
                        "mail.smtp.starttls.enable",
                        "false"
                );
                properties.setProperty(
                        "mail.smtp.ssl.enable",
                        "false"
                );
            }
        }
    }

}
