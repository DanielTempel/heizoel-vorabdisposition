package heizoel.backend.adapter.out.notification.whatsapp;

import heizoel.backend.adapter.out.notification.ConfirmationMessageContent;
import heizoel.backend.adapter.out.notification.twilio.TwilioMessageSender;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WhatsAppNotificationSenderTest {

    @Test
    void sendConfirmationRequestBuildsTemplateVariables() {
        ConfirmationProperties properties = new ConfirmationProperties();
        properties.setFrontendUrl("https://frontend.example");
        TwilioMessageSender twilioMessageSender = mock(TwilioMessageSender.class);
        WhatsAppNotificationSender sender = new WhatsAppNotificationSender(properties, twilioMessageSender);

        Order order = order();
        ConfirmationRequest confirmationRequest = confirmationRequest(order, CommunicationChannel.WHATSAPP);

        sender.sendConfirmationRequest(order, confirmationRequest);

        verify(twilioMessageSender).sendWhatsApp(
                "A-1024",
                "+491701234567",
                new ConfirmationMessageContent(
                        "Max Mustermann",
                        "A-1024",
                        "Heizoel",
                        "3000",
                        "12.06.2099",
                        "10:00 - 11:00",
                        "Beispielstrasse 12, 97070 Wuerzburg",
                        "https://frontend.example/confirmation/token-123"
                )
        );
    }

    private Order order() {
        return Order.create(
                Company.create("Test Company", "api-key", "http://callback.example"),
                "A-1024",
                Tour.of("17", "WUE-AB-123"),
                "Max Mustermann",
                null,
                "+491701234567",
                "Beispielstrasse 12, 97070 Wuerzburg",
                "Heizoel",
                3000,
                "95,40 EUR / 100 L"
        );
    }

    private ConfirmationRequest confirmationRequest(
            Order order,
            CommunicationChannel channel
    ) {
        return ConfirmationRequest.createPending(
                order,
                "token-123",
                channel,
                DeliverySlot.of(
                        LocalDate.of(2099, 6, 12),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                24
        );
    }
}
