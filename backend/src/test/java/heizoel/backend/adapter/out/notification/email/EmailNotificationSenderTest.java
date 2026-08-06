package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.application.exception.EmailSettingsNotConfiguredException;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.company.Company;
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

class EmailNotificationSenderTest {

    private static final Long COMPANY_ID = 1L;

    private CompanyEmailSettingsRepository
            companyEmailSettingsRepository;

    private CompanyMailSenderFactory companyMailSenderFactory;
    private ThymeleafConfirmationMailRenderer mailRenderer;

    private JavaMailSenderImpl mailSender;
    private CompanyEmailSettings settings;
    private Order order;
    private ConfirmationRequest confirmationRequest;

    private EmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        companyEmailSettingsRepository =
                mock(CompanyEmailSettingsRepository.class);

        companyMailSenderFactory =
                mock(CompanyMailSenderFactory.class);

        ConfirmationProperties confirmationProperties = mock(ConfirmationProperties.class);

        mailRenderer =
                mock(ThymeleafConfirmationMailRenderer.class);

        mailSender =
                mock(JavaMailSenderImpl.class);

        settings =
                mock(CompanyEmailSettings.class);

        Company company = mock(Company.class);

        order =
                mock(Order.class);

        confirmationRequest =
                mock(ConfirmationRequest.class);

        sender = new EmailNotificationSender(
                companyEmailSettingsRepository,
                companyMailSenderFactory,
                confirmationProperties,
                mailRenderer
        );

        when(company.getId()).thenReturn(COMPANY_ID);
        when(order.getCompany()).thenReturn(company);

        when(order.getExternalOrderId())
                .thenReturn("A-7120");

        when(order.getCustomerEmail())
                .thenReturn("customer@example.de");

        when(confirmationRequest.getToken())
                .thenReturn("confirmation-token");

        when(confirmationProperties.getFrontendUrl())
                .thenReturn("http://localhost:3000");

        when(settings.getFromAddress())
                .thenReturn("dispo@example.de");

        when(settings.getFromName())
                .thenReturn("Example Heizöl");
    }

    @Test
    void supportsEmailChannel() {
        assertThat(sender.channel())
                .isEqualTo(CommunicationChannel.EMAIL);
    }

    @Test
    void sendsConfirmationRequestUsingCompanySettings()
            throws Exception {

        String expectedUrl =
                "http://localhost:3000/confirmation/confirmation-token";

        String htmlBody =
                "<html><body>Confirmation mail</body></html>";

        MimeMessage message = createMimeMessage();

        when(mailRenderer.renderConfirmationRequestMail(
                order,
                confirmationRequest,
                expectedUrl
        )).thenReturn(htmlBody);

        configureMailSender(message);

        sender.sendConfirmationRequest(
                order,
                confirmationRequest
        );

        verify(companyEmailSettingsRepository)
                .findByCompanyId(COMPANY_ID);

        verify(companyMailSenderFactory)
                .create(COMPANY_ID, settings);

        verify(mailRenderer)
                .renderConfirmationRequestMail(
                        order,
                        confirmationRequest,
                        expectedUrl
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
                .isEqualTo("customer@example.de");

        assertThat(message.getSubject())
                .isEqualTo(
                        "Bitte bestätigen Sie Ihren Liefertermin"
                );
    }

    @Test
    void rendersConfirmedCustomerResponseMail()
            throws Exception {

        String expectedUrl =
                "http://localhost:3000/confirmation/confirmation-token";

        MimeMessage message = createMimeMessage();

        when(mailRenderer.renderCustomerConfirmedMail(
                order,
                confirmationRequest,
                expectedUrl
        )).thenReturn("<html>Confirmed</html>");

        configureMailSender(message);

        sender.sendCustomerResponseReceived(
                order,
                confirmationRequest,
                CustomerResponseType.CONFIRM
        );

        verify(mailRenderer)
                .renderCustomerConfirmedMail(
                        order,
                        confirmationRequest,
                        expectedUrl
                );

        verify(mailRenderer, never())
                .renderCustomerRejectedMail(
                        any(),
                        any(),
                        anyString()
                );

        verify(mailSender).send(message);

        assertThat(message.getSubject())
                .isEqualTo(
                        "Ihre Rückmeldung zur Lieferung wurde erhalten"
                );
    }

    @Test
    void rendersRejectedCustomerResponseMail()
            throws Exception {

        String expectedUrl =
                "http://localhost:3000/confirmation/confirmation-token";

        MimeMessage message = createMimeMessage();

        when(mailRenderer.renderCustomerRejectedMail(
                order,
                confirmationRequest,
                expectedUrl
        )).thenReturn("<html>Rejected</html>");

        configureMailSender(message);

        sender.sendCustomerResponseReceived(
                order,
                confirmationRequest,
                CustomerResponseType.REJECT
        );

        verify(mailRenderer)
                .renderCustomerRejectedMail(
                        order,
                        confirmationRequest,
                        expectedUrl
                );

        verify(mailRenderer, never())
                .renderCustomerConfirmedMail(
                        any(),
                        any(),
                        anyString()
                );

        verify(mailSender).send(message);
    }

    @Test
    void rejectsSendingWhenEmailSettingsAreMissing() {
        when(mailRenderer.renderConfirmationRequestMail(
                eq(order),
                eq(confirmationRequest),
                anyString()
        )).thenReturn("<html>Confirmation</html>");

        when(companyEmailSettingsRepository
                .findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                sender.sendConfirmationRequest(
                        order,
                        confirmationRequest
                )
        )
                .isInstanceOf(
                        EmailSettingsNotConfiguredException.class
                )
                .hasMessage(
                        "E-mail settings are not configured."
                );

        verifyNoInteractions(companyMailSenderFactory);
        verifyNoInteractions(mailSender);
    }

    @Test
    void wrapsMailSendingFailure()
            throws Exception {

        MimeMessage message = createMimeMessage();

        when(mailRenderer.renderConfirmationRequestMail(
                eq(order),
                eq(confirmationRequest),
                anyString()
        )).thenReturn("<html>Confirmation</html>");

        configureMailSender(message);

        doThrow(new MailSendException(
                "SMTP server rejected the message"
        ))
                .when(mailSender)
                .send(message);

        assertThatThrownBy(() ->
                sender.sendConfirmationRequest(
                        order,
                        confirmationRequest
                )
        )
                .isInstanceOf(
                        NotificationDeliveryException.class
                )
                .satisfies(throwable -> {
                    NotificationDeliveryException exception =
                            (NotificationDeliveryException) throwable;

                    assertThat(exception.getChannel())
                            .isEqualTo(
                                    CommunicationChannel.EMAIL
                            );

                    assertThat(exception.getMessage())
                            .isEqualTo(
                                    "Notification could not be delivered "
                                            + "for externalOrderId=A-7120"
                            );

                    assertThat(exception.getCause())
                            .isInstanceOf(
                                    MailSendException.class
                            );
                });
    }

    private void configureMailSender(
            MimeMessage message
    ) {
        when(companyEmailSettingsRepository
                .findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(settings));

        when(companyMailSenderFactory.create(
                COMPANY_ID,
                settings
        )).thenReturn(mailSender);

        when(mailSender.createMimeMessage())
                .thenReturn(message);
    }

    private MimeMessage createMimeMessage() {
        return new MimeMessage(
                Session.getInstance(new Properties())
        );
    }
}