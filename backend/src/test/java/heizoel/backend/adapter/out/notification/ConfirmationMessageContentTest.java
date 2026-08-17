package heizoel.backend.adapter.out.notification;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationMessageContentTest {

    @Test
    void fromBuildsFormattedSharedMessageContent() {
        Order order = Order.create(
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

        ConfirmationRequest confirmationRequest = ConfirmationRequest.create(
                order,
                "token-123",
                CommunicationChannel.WHATSAPP,
                DeliverySlot.of(
                        LocalDate.of(2099, 6, 12),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                Instant.parse("2099-06-10T08:00:00Z"),
                24
        );

        ConfirmationMessageContent content = ConfirmationMessageContent.from(
                order,
                confirmationRequest,
                "https://frontend.example"
        );

        assertThat(content).isEqualTo(new ConfirmationMessageContent(
                "Max Mustermann",
                "A-1024",
                "Heizoel",
                "3000",
                "12.06.2099",
                "10:00 - 11:00",
                "Beispielstrasse 12, 97070 Wuerzburg",
                "https://frontend.example/confirmation/token-123"
        ));
    }
}
