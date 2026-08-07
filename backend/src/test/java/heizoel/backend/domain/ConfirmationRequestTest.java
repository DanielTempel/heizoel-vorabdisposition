package heizoel.backend.domain;

import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationRequestTest {

    private static final Instant SENT_AT = Instant.parse("2026-08-07T08:00:00Z");
    private static final Instant DELIVERY_START = Instant.parse("2026-08-10T08:00:00Z");
    private static final DeliverySlot DELIVERY_SLOT = DeliverySlot.of(
            LocalDate.of(2026, 8, 10),
            LocalTime.of(10, 0),
            LocalTime.of(12, 0)
    );

    @Test
    void createPendingInitializesPendingInactiveRequest() {
        Order order = new Order();

        ConfirmationRequest request = ConfirmationRequest.createPending(
                order,
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_SLOT,
                24
        );

        assertThat(request.getOrder()).isSameAs(order);
        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(request.getDeliverySlot()).isEqualTo(DELIVERY_SLOT);
        assertThat(request.getResponseDeadlineHours()).isEqualTo(24);
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(request.isActive()).isFalse();
        assertThat(request.getSentAt()).isNull();
        assertThat(request.getExpiresAt()).isNull();
    }

    @Test
    void markSentTransitionsPendingRequestAndCalculatesDeadline() {
        ConfirmationRequest request = pendingRequest(24);

        request.markSent(SENT_AT);

        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(request.isActive()).isTrue();
        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
        assertThat(request.getExpiresAt()).isEqualTo(SENT_AT.plusSeconds(24 * 60 * 60));
    }

    @Test
    void calculateResponseDeadlineUsesConfiguredDeadlineBeforeDeliveryWindow() {
        ConfirmationRequest request = pendingRequest(24);

        assertThat(request.calculateResponseDeadline(SENT_AT))
                .isEqualTo(SENT_AT.plusSeconds(24 * 60 * 60));
    }

    @Test
    void calculateResponseDeadlineIsCappedAtDeliveryWindowStart() {
        ConfirmationRequest request = pendingRequest(96);

        assertThat(request.calculateResponseDeadline(SENT_AT))
                .isEqualTo(DELIVERY_START);
    }

    @Test
    void validateCanBeSentAtAcceptsInstantBeforeDeliveryWindow() {
        ConfirmationRequest request = pendingRequest(24);

        request.validateCanBeSentAt(DELIVERY_START.minusNanos(1));
    }

    @Test
    void validateCanBeSentAtRejectsDeliveryWindowThatAlreadyStarted() {
        ConfirmationRequest request = pendingRequest(24);

        assertThatThrownBy(() -> request.validateCanBeSentAt(DELIVERY_START))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");
    }

    @Test
    void markDeliveryFailedTransitionsPendingRequestToInactiveFailed() {
        ConfirmationRequest request = pendingRequest(24);

        request.markDeliveryFailed();

        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(request.isActive()).isFalse();
        assertThat(request.getSentAt()).isNull();
        assertThat(request.getExpiresAt()).isNull();
    }

    @Test
    void markInactiveDeactivatesSentRequestWithoutChangingDeliveryState() {
        ConfirmationRequest request = sentRequest();

        request.markInactive();

        assertThat(request.isActive()).isFalse();
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(request.getSentAt()).isEqualTo(SENT_AT);
    }

    @Test
    void isExpiredAtIsFalseWithoutDeadlineAndTrueAtDeadline() {
        ConfirmationRequest pending = pendingRequest(24);
        ConfirmationRequest sent = sentRequest();

        assertThat(pending.isExpiredAt(DELIVERY_START)).isFalse();
        assertThat(sent.isExpiredAt(sent.getExpiresAt().minusNanos(1))).isFalse();
        assertThat(sent.isExpiredAt(sent.getExpiresAt())).isTrue();
        assertThat(sent.isExpiredAt(sent.getExpiresAt().plusNanos(1))).isTrue();
    }

    @Test
    void updatePendingReplacesDuplicateRelevantDataWithoutChangingState() {
        ConfirmationRequest request = pendingRequest(24);
        DeliverySlot changedSlot = DeliverySlot.of(
                LocalDate.of(2026, 8, 11),
                LocalTime.of(13, 0),
                LocalTime.of(15, 0)
        );

        request.updatePending(CommunicationChannel.SMS, changedSlot, 48);

        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(request.getDeliverySlot()).isEqualTo(changedSlot);
        assertThat(request.getResponseDeadlineHours()).isEqualTo(48);
        assertThat(request.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(request.isActive()).isFalse();
    }

    @Test
    void sentRequestRejectsInvalidTransitions() {
        ConfirmationRequest request = sentRequest();

        assertThatThrownBy(() -> request.markSent(SENT_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending confirmation request can be marked as sent.");
        assertThatThrownBy(request::markDeliveryFailed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending confirmation request can be marked as failed.");
        assertThatThrownBy(() -> request.updatePending(
                CommunicationChannel.SMS,
                DELIVERY_SLOT,
                48
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending confirmation request can be updated.");
    }

    private ConfirmationRequest pendingRequest(int responseDeadlineHours) {
        return ConfirmationRequest.createPending(
                new Order(),
                "token",
                CommunicationChannel.EMAIL,
                DELIVERY_SLOT,
                responseDeadlineHours
        );
    }

    private ConfirmationRequest sentRequest() {
        ConfirmationRequest request = pendingRequest(24);
        request.markSent(SENT_AT);
        return request;
    }
}
