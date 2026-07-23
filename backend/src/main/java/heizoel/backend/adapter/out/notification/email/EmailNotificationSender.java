package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.adapter.out.notification.NotificationChannelSender;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.configuration.properties.MailProperties;
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
public class EmailNotificationSender implements NotificationChannelSender {

    private static final String SUBJECT_CONFIRMATION_REQUEST =
            "Bitte bestätigen Sie Ihren Liefertermin";

    private static final String SUBJECT_CUSTOMER_RESPONSE_RECEIVED =
            "Ihre Rückmeldung zur Lieferung wurde erhalten";


    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ConfirmationProperties confirmationProperties;
    private final ThymeleafConfirmationMailRenderer mailRenderer;

    @Override
    public CommunicationChannel channel() {
        return CommunicationChannel.EMAIL;
    }

    @Override
    public void sendConfirmationRequest(
            Order order,
            ConfirmationRequest confirmationRequest
    ) {
        String confirmationUrl = buildConfirmationUrl(confirmationRequest);

        String htmlBody = mailRenderer.renderConfirmationRequestMail(
                order,
                confirmationRequest,
                confirmationUrl
        );

        sendHtmlMail(
                order,
                SUBJECT_CONFIRMATION_REQUEST,
                htmlBody
        );
    }

    @Override
    public void sendCustomerResponseReceived(
            Order order,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {
        String confirmationUrl = buildConfirmationUrl(confirmationRequest);

        String htmlBody = switch (responseType) {
            case CONFIRM -> mailRenderer.renderCustomerConfirmedMail(
                    order,
                    confirmationRequest,
                    confirmationUrl
            );
            case REJECT -> mailRenderer.renderCustomerRejectedMail(
                    order,
                    confirmationRequest,
                    confirmationUrl
            );
        };

        sendHtmlMail(
                order,
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
            Order order,
            String subject,
            String htmlBody
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true,"UTF-8");

            helper.setFrom(mailProperties.getFrom());
            helper.setTo(order.getCustomerEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addInline(
                    "minovaLogo",
                    new ClassPathResource("assets/minova-logo.png")
            );

            mailSender.send(message);

        } catch (MessagingException | MailException ex) {
            throw new NotificationDeliveryException(
                    CommunicationChannel.EMAIL,
                    "Notification could not be delivered for externalOrderId="
                            + order.getExternalOrderId(),
                    ex
            );
        }
    }
}
