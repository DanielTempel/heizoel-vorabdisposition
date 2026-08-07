package heizoel.backend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void createPendingPendingSetsDeliveryStatusToPending() {
        ConfirmationRequest request = pendingRequest(new Order());

        assertThat(request.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.PENDING);
    }

    @Test
    void createPendingPendingCreatesInactiveRequest() {
        ConfirmationRequest request = pendingRequest(new Order());

        assertThat(request.isActive()).isFalse();
    }

    @Test
    void createPendingPendingLeavesDeliveryTimestampsNull() {
        ConfirmationRequest request = pendingRequest(new Order());

        assertThat(request.getSentAt()).isNull();
        assertThat(request.getExpiresAt()).isNull();
    }

    @Test
    void markSentChangesPendingRequestToSent() {
        ConfirmationRequest request = pendingRequest(new Order());

        request.markSent(SENT_AT);

        assertThat(request.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.SENT);
    }

    @Test
    void markSentActivatesRequest() {
        ConfirmationRequest request = pendingRequest(new Order());

        request.markSent(SENT_AT);

        assertThat(request.isActive()).isTrue();
    }

    @Test
    void markSentSetsDeliveryTimestamps() {
        ConfirmationRequest request = pendingRequest(new Order());

        request.markSent(SENT_AT);

        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
        assertThat(request.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void markDeliveryFailedChangesPendingRequestToFailed() {
        ConfirmationRequest request = pendingRequest(new Order());

        request.markDeliveryFailed();

        assertThat(request.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.FAILED);
    }

    @Test
    void markDeliveryFailedLeavesRequestInactive() {
        ConfirmationRequest request = pendingRequest(new Order());

        request.markDeliveryFailed();

        assertThat(request.isActive()).isFalse();
    }

    @Test
    void markSentIsForbiddenForSentRequest() {
        ConfirmationRequest request = sentRequest(new Order());

        assertThatThrownBy(() -> request.markSent(SENT_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markDeliveryFailedIsForbiddenForSentRequest() {
        ConfirmationRequest request = sentRequest(new Order());

        assertThatThrownBy(request::markDeliveryFailed)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createPendingSentPreservesReadyRequestBehavior() {
        Order order = new Order();

        ConfirmationRequest request = sentRequest(order);

        assertThat(request.getOrder()).isSameAs(order);
        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.getDeliveryStatus())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(request.isActive()).isTrue();
        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
        assertThat(request.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void markInactiveMakesRequestInactive() {
        ConfirmationRequest request = sentRequest(new Order());

        request.markInactive();

        assertThat(request.isActive()).isFalse();
    }

    @Test
    void requestExpiresAtDeadline() {
        ConfirmationRequest request = sentRequest(new Order());

        assertThat(request.isExpiredAt(EXPIRES_AT.minusNanos(1))).isFalse();
        assertThat(request.isExpiredAt(EXPIRES_AT)).isTrue();
    }

    @Test
    void hasSameDataComparesAllDuplicateRelevantRequestData() {
        ConfirmationRequest request = sentRequest(new Order());

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

    private ConfirmationRequest pendingRequest(Order order) {
        return ConfirmationRequest.createPending(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_SLOT,
                24
        );
    }

    private ConfirmationRequest sentRequest(Order order) {
        return ConfirmationRequest.createSent(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_SLOT,
                SENT_AT,
                24
        );
    }
}
