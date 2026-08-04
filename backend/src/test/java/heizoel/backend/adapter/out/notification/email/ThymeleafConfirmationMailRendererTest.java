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
                order(),
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        );

        assertThat(html)
                .contains("13.06.2026")
                .contains("10:15")
                .contains("11:45")
                .contains("11.06.2026, 18 Uhr")
                .doesNotContain("18:09 Uhr");
    }

    @Test
    void shouldRenderInlineLogoCidInAllMailTemplates() {
        ThymeleafConfirmationMailRenderer renderer = new ThymeleafConfirmationMailRenderer(
                templateEngine()
        );

        Order order = order();
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

    private Order order() {
        return Order.create(
                Company.create(
                        "Company", "api-key-hash", "http://localhost/callback"
                ),
                "A-123", Tour.of("17", "WÜ-AB 123"),
                "Max Muller", null, null,
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
                        LocalDate.of(2026, 6, 13),
                        LocalTime.of(10, 15),
                        LocalTime.of(11, 45)
                ),
                Instant.parse("2026-06-11T15:09:00Z"),
                1
        );
    }
}

