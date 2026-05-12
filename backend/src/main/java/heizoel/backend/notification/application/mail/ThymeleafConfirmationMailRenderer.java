package heizoel.backend.notification.application.mail;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class ThymeleafConfirmationMailRenderer {

    private static final String TEMPLATE_NAME = "mail/confirmation-request";

    private final TemplateEngine templateEngine;

    public String renderConfirmationRequestMail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = new Context();

        context.setVariable("customerName", orderSnapshot.getCustomerName());
        context.setVariable("deliveryAddress", orderSnapshot.getDeliveryAddress());
        context.setVariable("product", orderSnapshot.getProduct());
        context.setVariable("quantityLiters", orderSnapshot.getQuantityLiters());
        context.setVariable("deliveryDate", confirmationRequest.getDeliveryDate());
        context.setVariable("deliveryWindowStart", confirmationRequest.getDeliveryWindowStart());
        context.setVariable("deliveryWindowEnd", confirmationRequest.getDeliveryWindowEnd());
        context.setVariable("confirmationUrl", confirmationUrl);

        return templateEngine.process(TEMPLATE_NAME, context);
    }

}
