package heizoel.backend.notification.application.mail;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import heizoel.backend.notification.infrastrukture.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpConfirmationNotificationService implements ConfirmationNotificationService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ConfirmationProperties confirmationProperties;
    private final ThymeleafConfirmationMailRenderer mailRenderer;

    @Override
    public void sendConfirmationRequestEmail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        try {
            String confirmationUrl = confirmationProperties.getFrontendBaseUrl()
                    + "/confirmation/"
                    + confirmationRequest.getToken();

            String htmlBody = mailRenderer.renderConfirmationRequestMail(
                    orderSnapshot,
                    confirmationRequest,
                    confirmationUrl
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(orderSnapshot.getCustomerEmail());
            helper.setSubject("Bitte bestätigen Sie Ihren Heizöl-Liefertermin");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailSendException("Could not prepare confirmation e-mail.", ex);
        }
    }


}
