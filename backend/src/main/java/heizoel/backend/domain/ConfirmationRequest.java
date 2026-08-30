package heizoel.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(
        name = "confirmation_request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_confirmation_request_token",
                        columnNames = "token"
                )
        },
        indexes = {
                @Index(
                        name = "idx_confirmation_request_order_snapshot_id",
                        columnList = "order_snapshot_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_snapshot_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_channel", nullable = false, length = 20)
    private CommunicationChannel communicationChannel;

    @Embedded
    private DeliverySlot deliverySlot;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "response_deadline_hours", nullable = false)
    private Integer responseDeadlineHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private NotificationDeliveryStatus deliveryStatus;

    public static ConfirmationRequest createPending(
            Order order,
            String token,
            CommunicationChannel communicationChannel,
            DeliverySlot deliverySlot,
            Integer responseDeadlineHours
    ) {
        ConfirmationRequest request = new ConfirmationRequest();
        request.order = order;
        request.token = token;
        request.communicationChannel = communicationChannel;
        request.deliverySlot = deliverySlot;
        request.responseDeadlineHours = responseDeadlineHours;

        request.deliveryStatus = NotificationDeliveryStatus.PENDING;
        request.active = false;
        request.sentAt = null;
        request.expiresAt = null;

        return request;
    }

    public boolean isSent() {
        return deliveryStatus == NotificationDeliveryStatus.SENT;
    }

    public void markSent(Instant sentAt) {
        if (deliveryStatus != NotificationDeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending confirmation request can be marked as sent."
            );
        }

        this.sentAt = sentAt;
        this.expiresAt = calculateResponseDeadline(sentAt);
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.active = true;
    }

    public Instant calculateResponseDeadline(Instant sentAt) {
        deliverySlot.validateStartsAfter(sentAt);

        Instant requestedDeadline =
                sentAt.plus(Duration.ofHours(responseDeadlineHours));

        Instant deliveryStartsAt = deliverySlot.startsAt();

        return requestedDeadline.isBefore(deliveryStartsAt)
                ? requestedDeadline
                : deliveryStartsAt;
    }

    public void markInactive() {
        this.active = false;
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }


    public boolean hasSameData(
            DeliverySlot deliverySlot,
            CommunicationChannel communicationChannel,
            Integer responseDeadlineHours
    ) {
        return this.deliverySlot.equals(deliverySlot)
                && this.communicationChannel == communicationChannel
                && this.responseDeadlineHours.equals(responseDeadlineHours);
    }

    public boolean isPending() {
        return deliveryStatus == NotificationDeliveryStatus.PENDING;
    }
    public void updatePending(
            CommunicationChannel communicationChannel,
            DeliverySlot deliverySlot,
            Integer responseDeadlineHours
    ) {
        if (deliveryStatus != NotificationDeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending confirmation request can be updated."
            );
        }

        this.communicationChannel = communicationChannel;
        this.deliverySlot = deliverySlot;
        this.responseDeadlineHours = responseDeadlineHours;
    }


    public boolean isDeliveryFailed() {
        return deliveryStatus == NotificationDeliveryStatus.FAILED;
    }

    public void markDeliveryFailed() {
        if (deliveryStatus != NotificationDeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending confirmation request can be marked as failed."
            );
        }

        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
        this.active = false;
        this.sentAt = null;
        this.expiresAt = null;
    }
}

