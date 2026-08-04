package heizoel.backend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationRequestTest {

    private static final Instant SENT_AT = Instant.parse("2026-07-01T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-02T10:00:00Z");
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 7, 3);
    private static final LocalTime WINDOW_START = LocalTime.of(10, 0);
    private static final LocalTime WINDOW_END = LocalTime.of(12, 0);
    private static final DeliverySlot DELIVERY_SLOT = DeliverySlot.of(
            DELIVERY_DATE,
            WINDOW_START,
            WINDOW_END
    );

    @Test
    void createBuildsActiveRequestFromReadyValues() {
        Order order = new Order();

        ConfirmationRequest request = request(order);

        assertThat(request.getOrder()).isSameAs(order);
        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.isActive()).isTrue();
        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
        assertThat(request.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void markInactiveMakesRequestInactive() {
        ConfirmationRequest request = request(new Order());

        request.markInactive();

        assertThat(request.isActive()).isFalse();
    }

    @Test
    void requestExpiresAtDeadline() {
        ConfirmationRequest request = request(new Order());

        assertThat(request.isExpiredAt(EXPIRES_AT.minusNanos(1))).isFalse();
        assertThat(request.isExpiredAt(EXPIRES_AT)).isTrue();
    }

    @Test
    void hasSameDataComparesAllDuplicateRelevantRequestData() {
        ConfirmationRequest request = request(new Order());

        assertThat(request.hasSameData(
                DELIVERY_SLOT,
                CommunicationChannel.EMAIL,
                24
        )).isTrue();
        assertThat(request.hasSameData(
                DELIVERY_SLOT,
                CommunicationChannel.SMS,
                24
        )).isFalse();
    }

    private ConfirmationRequest request(Order order) {
        return ConfirmationRequest.create(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_SLOT,
                SENT_AT,
                24
        );
    }
}
