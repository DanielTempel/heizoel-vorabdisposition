package heizoel.backend.notification.application.mail;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
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

        OrderSnapshot orderSnapshot = orderSnapshot();
        ConfirmationRequest confirmationRequest = confirmationRequest();

        assertThat(renderer.renderConfirmationRequestMail(
                orderSnapshot,
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        )).contains("src=\"cid:minovaLogo\"");

        assertThat(renderer.renderCustomerConfirmedMail(
                orderSnapshot,
                confirmationRequest,
                "http://localhost:3000/confirmation/token"
        )).contains("src=\"cid:minovaLogo\"");

        assertThat(renderer.renderCustomerRejectedMail(
                orderSnapshot,
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

    private OrderSnapshot orderSnapshot() {
        OrderSnapshot orderSnapshot = new OrderSnapshot();
        orderSnapshot.setCustomerName("Max Muller");
        orderSnapshot.setExternalOrderId("A-123");
        orderSnapshot.setDeliveryAddress("Beispielstrasse 12, 97070 Wuerzburg");
        orderSnapshot.setProduct("Heizoel");
        orderSnapshot.setQuantityLiters(3000);
        orderSnapshot.setPriceDisplayText("100 EUR");
        return orderSnapshot;
    }

    private ConfirmationRequest confirmationRequest() {
        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.setDeliveryDate(LocalDate.of(2026, 6, 12));
        confirmationRequest.setDeliveryWindowStart(LocalTime.of(10, 0));
        confirmationRequest.setDeliveryWindowEnd(LocalTime.of(11, 0));
        confirmationRequest.setExpiresAt(Instant.parse("2026-06-11T16:09:00Z"));
        return confirmationRequest;
    }
}
