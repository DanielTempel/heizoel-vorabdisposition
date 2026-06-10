package heizoel.backend.notification.application.mail;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.exceptions.notification.EmailSendingException;
import heizoel.backend.notification.application.interfaces.EmailConfirmationSender;
import heizoel.backend.notification.infrastructure.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailConfirmationNotificationService implements EmailConfirmationSender {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ConfirmationProperties confirmationProperties;
    private final ThymeleafConfirmationMailRenderer mailRenderer;

    @Override
    public void send(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        try {
            String confirmationUrl = confirmationProperties.getFrontendUrl()
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
            helper.setSubject("Bitte bestätigen Sie Ihren Liefertermin");
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new EmailSendingException(
                    "The confirmation e-mail could not be sent for externalOrderId="
                            + orderSnapshot.getExternalOrderId(),
                    ex
            );
        }
    }


}
