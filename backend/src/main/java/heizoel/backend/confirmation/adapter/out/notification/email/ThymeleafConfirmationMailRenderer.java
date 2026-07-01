package heizoel.backend.confirmation.adapter.out.notification.email;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ThymeleafConfirmationMailRenderer {

    private static final String TEMPLATE_CONFIRMATION_REQUEST =
            "mail/confirmation-request";

    private static final String TEMPLATE_CUSTOMER_CONFIRMED =
            "mail/customer-response-confirmed";

    private static final String TEMPLATE_CUSTOMER_REJECTED =
            "mail/customer-response-rejected";

    private final TemplateEngine templateEngine;

    public String renderConfirmationRequestMail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                orderSnapshot,
                confirmationRequest,
                confirmationUrl
        );

        context.setVariable(
                "responseDeadline",
                formatDeadline(confirmationRequest)
        );

        return templateEngine.process(TEMPLATE_CONFIRMATION_REQUEST, context);
    }

    public String renderCustomerConfirmedMail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                orderSnapshot,
                confirmationRequest,
                confirmationUrl
        );

        return templateEngine.process(TEMPLATE_CUSTOMER_CONFIRMED, context);
    }

    public String renderCustomerRejectedMail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                orderSnapshot,
                confirmationRequest,
                confirmationUrl
        );

        return templateEngine.process(TEMPLATE_CUSTOMER_REJECTED, context);
    }

    private Context createBaseContext(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = new Context(Locale.GERMANY);

        context.setVariable("customerName", orderSnapshot.getCustomerName());
        context.setVariable("externalOrderId", orderSnapshot.getExternalOrderId());
        context.setVariable("deliveryAddress", orderSnapshot.getDeliveryAddress());
        context.setVariable("product", orderSnapshot.getProduct());
        context.setVariable("quantityLiters", orderSnapshot.getQuantityLiters());
        context.setVariable("priceDisplayText", orderSnapshot.getPriceDisplayText());
        context.setVariable("deliveryDate", confirmationRequest.getDeliveryDate());
        context.setVariable("deliveryWindowStart", confirmationRequest.getDeliveryWindowStart());
        context.setVariable("deliveryWindowEnd", confirmationRequest.getDeliveryWindowEnd());

        context.setVariable("confirmationUrl", confirmationUrl);

        return context;
    }


    private String formatDeadline(ConfirmationRequest confirmationRequest) {
        return confirmationRequest.getExpiresAt()
                .atZone(ZoneId.of("Europe/Berlin"))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, H 'Uhr'", Locale.GERMANY));
    }

}

