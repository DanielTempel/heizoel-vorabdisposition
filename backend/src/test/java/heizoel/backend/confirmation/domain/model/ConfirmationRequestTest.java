package heizoel.backend.confirmation.domain.model;

import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.OrderSnapshot;
import heizoel.backend.domain.model.enumeration.CommunicationChannel;
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

    @Test
    void createBuildsActiveRequestFromReadyValues() {
        OrderSnapshot orderSnapshot = new OrderSnapshot();

        ConfirmationRequest request = request(orderSnapshot);

        assertThat(request.getOrderSnapshot()).isSameAs(orderSnapshot);
        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.isActive()).isTrue();
        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
        assertThat(request.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void markInactiveMakesRequestInactive() {
        ConfirmationRequest request = request(new OrderSnapshot());

        request.markInactive();

        assertThat(request.isActive()).isFalse();
    }

    @Test
    void requestExpiresAtDeadline() {
        ConfirmationRequest request = request(new OrderSnapshot());

        assertThat(request.isExpiredAt(EXPIRES_AT.minusNanos(1))).isFalse();
        assertThat(request.isExpiredAt(EXPIRES_AT)).isTrue();
    }

    @Test
    void hasSameDataComparesAllDuplicateRelevantRequestData() {
        ConfirmationRequest request = request(new OrderSnapshot());

        assertThat(request.hasSameData(
                DELIVERY_DATE,
                WINDOW_START,
                WINDOW_END,
                CommunicationChannel.EMAIL,
                24
        )).isTrue();
        assertThat(request.hasSameData(
                DELIVERY_DATE,
                WINDOW_START,
                WINDOW_END,
                CommunicationChannel.SMS,
                24
        )).isFalse();
    }

    private ConfirmationRequest request(OrderSnapshot orderSnapshot) {
        return ConfirmationRequest.create(
                orderSnapshot,
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_DATE,
                WINDOW_START,
                WINDOW_END,
                SENT_AT,
                EXPIRES_AT,
                24
        );
    }
}
