package heizoel.backend.application.service.settings;

import heizoel.backend.adapter.out.notification.email.CompanyMailSenderFactory;
import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.EmailConnectionTestException;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.port.in.settings.TestEmailConnectionUseCase;
import heizoel.backend.domain.company.CompanyEmailSettings;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestEmailConnectionService implements TestEmailConnectionUseCase {

    private final CompanyEmailSettingsRepository companyEmailSettingsRepository;
    private final CompanyMailSenderFactory companyMailSenderFactory;

    @Override
    public void testEmailConnection(CompanyContext companyContext ) {

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
            mailSender.testConnection();
        } catch (MessagingException exception) {
            log.warn(
                    "SMTP connection test failed for companyId={}",
                    companyId,
                    exception
            );

            throw new EmailConnectionTestException(
                    "SMTP connection could not be established.",
                    exception
            );
        }
    }

}
