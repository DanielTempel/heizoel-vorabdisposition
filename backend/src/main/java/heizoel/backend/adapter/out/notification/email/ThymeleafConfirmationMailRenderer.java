package heizoel.backend.adapter.out.notification.email;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ThymeleafConfirmationMailRenderer {

    private static final Locale MAIL_LOCALE = Locale.GERMANY;
    private static final DateTimeFormatter DELIVERY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", MAIL_LOCALE);
    private static final DateTimeFormatter DELIVERY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", MAIL_LOCALE);

    private static final String TEMPLATE_CONFIRMATION_REQUEST = "mail/confirmation-request";
    private static final String TEMPLATE_CUSTOMER_CONFIRMED = "mail/customer-response-confirmed";
    private static final String TEMPLATE_CUSTOMER_REJECTED = "mail/customer-response-rejected";

    private final TemplateEngine templateEngine;

    public String renderConfirmationRequestMail(
            Order order,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                order,
                confirmationRequest,
                confirmationUrl
        );

        return templateEngine.process(TEMPLATE_CONFIRMATION_REQUEST, context);
    }

    public String renderCustomerConfirmedMail(
            Order order,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                order,
                confirmationRequest,
                confirmationUrl
        );

        return templateEngine.process(TEMPLATE_CUSTOMER_CONFIRMED, context);
    }

    public String renderCustomerRejectedMail(
            Order order,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = createBaseContext(
                order,
                confirmationRequest,
                confirmationUrl
        );

        return templateEngine.process(TEMPLATE_CUSTOMER_REJECTED, context);
    }

    private Context createBaseContext(
            Order order,
            ConfirmationRequest confirmationRequest,
            String confirmationUrl
    ) {
        Context context = new Context(MAIL_LOCALE);
        DeliverySlot deliverySlot = confirmationRequest.getDeliverySlot();

        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("externalOrderId", order.getExternalOrderId());
        context.setVariable("deliveryAddress", order.getDeliveryAddress());
        context.setVariable("product", order.getProduct());
        context.setVariable("quantityLiters", order.getQuantityLiters());
        context.setVariable("priceDisplayText", order.getPriceDisplayText());
        context.setVariable(
                "deliveryDate",
                deliverySlot.getDate().format(DELIVERY_DATE_FORMATTER)
        );
        context.setVariable(
                "deliveryWindowStart",
                deliverySlot.getStart().format(DELIVERY_TIME_FORMATTER)
        );
        context.setVariable(
                "deliveryWindowEnd",
                deliverySlot.getEnd().format(DELIVERY_TIME_FORMATTER)
        );

        context.setVariable("confirmationUrl", confirmationUrl);

        return context;
    }


}

