package heizoel.backend.confirmation.adapter.out.notification.email;

import heizoel.backend.confirmation.domain.model.CustomerResponseType;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.infrastructure.properties.ConfirmationProperties;
import heizoel.backend.shared.exception.EmailSendingException;
import heizoel.backend.confirmation.application.port.out.EmailSender;
import heizoel.backend.confirmation.infrastructure.properties.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderImpl implements EmailSender {

    private static final String SUBJECT_CONFIRMATION_REQUEST =
            "Bitte bestÃ¤tigen Sie Ihren Liefertermin";

    private static final String SUBJECT_CUSTOMER_RESPONSE_RECEIVED =
            "Ihre RÃ¼ckmeldung zur HeizÃ¶l-Lieferung wurde erhalten";


    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ConfirmationProperties confirmationProperties;
    private final ThymeleafConfirmationMailRenderer mailRenderer;


    @Override
    public void sendConfirmationRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        String confirmationUrl = buildConfirmationUrl(confirmationRequest);

        String htmlBody = mailRenderer.renderConfirmationRequestMail(
                orderSnapshot,
                confirmationRequest,
                confirmationUrl
        );

        sendHtmlMail(
                orderSnapshot,
                SUBJECT_CONFIRMATION_REQUEST,
                htmlBody
        );
    }

    @Override
    public void sendCustomerResponseReceived(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        String confirmationUrl = buildConfirmationUrl(confirmationRequest);

        String htmlBody = switch (responseType) {
            case CONFIRM -> mailRenderer.renderCustomerConfirmedMail(
                    orderSnapshot,
                    confirmationRequest,
                    confirmationUrl
            );
            case REJECT -> mailRenderer.renderCustomerRejectedMail(
                    orderSnapshot,
                    confirmationRequest,
                    confirmationUrl
            );
        };

        sendHtmlMail(
                orderSnapshot,
                SUBJECT_CUSTOMER_RESPONSE_RECEIVED,
                htmlBody
        );
    }

    private String buildConfirmationUrl(ConfirmationRequest confirmationRequest) {
        return confirmationProperties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();
    }

    private void sendHtmlMail(
            OrderSnapshot orderSnapshot,
            String subject,
            String htmlBody
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true,"UTF-8");

            helper.setFrom(mailProperties.getFrom());
            helper.setTo(orderSnapshot.getCustomerEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addInline(
                    "minovaLogo",
                    new ClassPathResource("assets/minova-logo.png")
            );

            mailSender.send(message);

        } catch (MessagingException | MailException ex) {
            throw new EmailSendingException(
                    "E-mail could not be sent for externalOrderId="
                            + orderSnapshot.getExternalOrderId(),
                    ex
            );
        }
    }
}
