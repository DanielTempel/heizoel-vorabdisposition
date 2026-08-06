package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.notification.email.CompanyMailSenderFactory;
import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.exception.TestEmailDeliveryException;
import heizoel.backend.application.port.in.settings.SendTestEmailUseCase;
import heizoel.backend.domain.company.CompanyEmailSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendTestEmailService implements SendTestEmailUseCase {

    private static final String SUBJECT =
            "SMTP-Konfiguration erfolgreich getestet";

    private static final String BODY = """
            Dies ist eine Test-E-Mail aus dem Avisierungsdashboard.

            Die Nachricht wurde über die gespeicherten SMTP-Einstellungen versendet.
            Der SMTP-Server hat die Nachricht erfolgreich angenommen.
            """;

    private final CompanyEmailSettingsRepository companyEmailSettingsRepository;
    private final CompanyMailSenderFactory companyMailSenderFactory;

    @Override
    public void sendTestEmail(CompanyContext companyContext) {
        Long companyId = companyContext.companyId();

        CompanyEmailSettings settings =
                companyEmailSettingsRepository
                        .findByCompanyId(companyId)
                        .orElseThrow(() ->
                                new EmailSettingsNotConfiguredException(
                                        "E-mail settings are not configured."
                                )
                        );

        JavaMailSenderImpl mailSender =
                companyMailSenderFactory.create(
                        companyId,
                        settings
                );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(
                    settings.getFromAddress(),
                    settings.getFromName()
            );

            helper.setTo(settings.getFromAddress());
            helper.setSubject(SUBJECT);
            helper.setText(BODY);

            mailSender.send(message);

        } catch (
                MessagingException
                | UnsupportedEncodingException
                | MailException exception
        ) {
            log.warn(
                    "Test e-mail delivery failed for companyId={}",
                    companyId,
                    exception
            );

            throw new TestEmailDeliveryException(
                    "Test e-mail could not be delivered.",
                    exception
            );
        }
    }

}
