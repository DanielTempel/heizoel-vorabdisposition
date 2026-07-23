package heizoel.backend.domain;

import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
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

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "response_deadline_hours", nullable = false)
    private Integer responseDeadlineHours;

    public static ConfirmationRequest create(
            Order order,
            String token,
            CommunicationChannel communicationChannel,
            DeliverySlot deliverySlot,
            Instant sentAt,
            Integer responseDeadlineHours
    ) {
        Instant deliveryStartsAt = deliverySlot.startsAt();

        if (!deliveryStartsAt.isAfter(sentAt)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }

        Instant requestedExpiresAt = sentAt.plus(
                Duration.ofHours(responseDeadlineHours)
        );

        Instant effectiveExpiresAt = requestedExpiresAt.isBefore(deliveryStartsAt)
                ? requestedExpiresAt
                : deliveryStartsAt;

        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.order = order;
        confirmationRequest.token = token;
        confirmationRequest.communicationChannel = communicationChannel;
        confirmationRequest.deliverySlot = deliverySlot;
        confirmationRequest.active = true;
        confirmationRequest.sentAt = sentAt;
        confirmationRequest.expiresAt = effectiveExpiresAt;
        confirmationRequest.responseDeadlineHours = responseDeadlineHours;
        return confirmationRequest;
    }

    public void markInactive() {
        this.active = false;
    }

    public boolean isExpiredAt(Instant now) {
        return !this.expiresAt.isAfter(now);
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

}

