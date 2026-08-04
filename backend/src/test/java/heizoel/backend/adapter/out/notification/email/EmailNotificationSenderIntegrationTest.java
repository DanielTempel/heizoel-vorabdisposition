package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.domain.*;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.configuration.properties.MailProperties;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = {
                EmailNotificationSender.class,
                ThymeleafConfirmationMailRenderer.class,
                MailProperties.class,
                ConfirmationProperties.class,
                EmailNotificationSenderIntegrationTest.TestConfig.class
        },
        properties = {
                "heizoel.mail.from=no-reply@heizoel.local",
                "heizoel.confirmation.frontend-url=http://localhost:3000"
        }
)
class EmailNotificationSenderIntegrationTest {

    @Autowired
    EmailNotificationSender emailSender;

    @MockitoBean
    JavaMailSender mailSender;

    MimeMessage message;

    @BeforeEach
    void setUp() {
        message = new MimeMessage(Session.getInstance(new Properties()));

        when(mailSender.createMimeMessage())
                .thenReturn(message);
    }

    @Test
    void sendConfirmationRequest_rendersTemplateAndAttachesMinovaLogoAsInlineImage() throws Exception {
        emailSender.sendConfirmationRequest(order(), confirmationRequest());

        verify(mailSender).send(message);
        message.saveChanges();

        assertThat(message.getSubject()).contains("Liefertermin");
        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@heizoel.local");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("daniel@example.com");
        assertThat(flattenText(message.getContent()))
                .contains("cid:minovaLogo")
                .contains("http://localhost:3000/confirmation/token");
        assertThat(containsInlinePartWithContentId(message.getContent(), "minovaLogo")).isTrue();
    }

    @Test
    void sendCustomerResponseReceived_confirmed_rendersFollowUpTemplateAndAttachesLogo() throws Exception {
        emailSender.sendCustomerResponseReceived(
                order(),
                confirmationRequest(),
                CustomerResponseType.CONFIRM
        );

        verify(mailSender).send(message);
        message.saveChanges();

        assertThat(message.getSubject()).contains("R");
        assertThat(flattenText(message.getContent()))
                .contains("Liefertermin best")
                .contains("cid:minovaLogo");
        assertThat(containsInlinePartWithContentId(message.getContent(), "minovaLogo")).isTrue();
    }

    @Test
    void sendCustomerResponseReceived_rejected_rendersFollowUpTemplateAndAttachesLogo() throws Exception {
        emailSender.sendCustomerResponseReceived(
                order(),
                confirmationRequest(),
                CustomerResponseType.REJECT
        );

        verify(mailSender).send(message);
        message.saveChanges();

        assertThat(flattenText(message.getContent()))
                .contains("Liefertermin abgelehnt")
                .contains("cid:minovaLogo");
        assertThat(containsInlinePartWithContentId(message.getContent(), "minovaLogo")).isTrue();
    }

    private boolean containsInlinePartWithContentId(Object content, String contentId) throws Exception {
        if (!(content instanceof Multipart multipart)) {
            return false;
        }

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String[] contentIds = bodyPart.getHeader("Content-ID");

            if (contentIds != null) {
                for (String currentContentId : contentIds) {
                    if (currentContentId.contains(contentId)) {
                        return true;
                    }
                }
            }

            if (containsInlinePartWithContentId(bodyPart.getContent(), contentId)) {
                return true;
            }
        }

        return false;
    }

    private String flattenText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }

        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(flattenText(multipart.getBodyPart(i).getContent()));
            }

            return text.toString();
        }

        return "";
    }

    private Order order() {
        return Order.create(
                Company.create(
                        "Company", "api-key-hash", "http://localhost/callback"
                ),
                "A-MAIL-1", Tour.of("17", "WÜ-AB 123"),
                "Max Muller", "daniel@example.com", null,
                "Beispielstrasse 12, 97070 Wuerzburg",
                "Heizoel", 3000, "100 EUR"
        );
    }

    private ConfirmationRequest confirmationRequest() {
        return ConfirmationRequest.create(
                order(),
                "token",
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2026, 6, 12),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                Instant.parse("2026-06-11T15:09:00Z"),
                1
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        SpringTemplateEngine templateEngine() {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding("UTF-8");

            SpringTemplateEngine templateEngine = new SpringTemplateEngine();
            templateEngine.setTemplateResolver(resolver);
            return templateEngine;
        }
    }
}

