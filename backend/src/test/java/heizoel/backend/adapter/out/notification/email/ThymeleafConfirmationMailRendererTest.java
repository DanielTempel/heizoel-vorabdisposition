package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafConfirmationMailRendererTest {

    @Test
    void shouldRenderResponseDeadlineWithoutMinutes() {
        ThymeleafConfirmationMailRenderer renderer = new ThymeleafConfirmationMailRenderer(
                templateEngine()
        );

        ConfirmationRequest confirmationRequest = confirmationRequest();

        String html = renderer.renderConfirmationRequestMail(
                orderSnapshot(),
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        );

        assertThat(html)
                .contains("11.06.2026, 18 Uhr")
                .doesNotContain("18:09 Uhr");
    }

    @Test
    void shouldRenderInlineLogoCidInAllMailTemplates() {
        ThymeleafConfirmationMailRenderer renderer = new ThymeleafConfirmationMailRenderer(
                templateEngine()
        );

        Order order = orderSnapshot();
        ConfirmationRequest confirmationRequest = confirmationRequest();

        assertThat(renderer.renderConfirmationRequestMail(
                order,
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        )).contains("src=\"cid:minovaLogo\"");

        assertThat(renderer.renderCustomerConfirmedMail(
                order,
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        )).contains("src=\"cid:minovaLogo\"");

        assertThat(renderer.renderCustomerRejectedMail(
                order,
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        )).contains("src=\"cid:minovaLogo\"");
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private Order orderSnapshot() {
        return Order.create(
                Company.create(
                        "Company", "api-key-hash", "http://localhost/callback"
                ),
                "A-123", "Max Muller", null, null,
                "Beispielstrasse 12, 97070 Wuerzburg",
                "Heizoel", 3000, "100 EUR"
        );
    }

    private ConfirmationRequest confirmationRequest() {
        return ConfirmationRequest.create(
                orderSnapshot(),
                "token",
                CommunicationChannel.EMAIL,
                LocalDate.of(2026, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                Instant.parse("2026-06-11T15:00:00Z"),
                Instant.parse("2026-06-11T16:09:00Z"),
                24
        );
    }
}

