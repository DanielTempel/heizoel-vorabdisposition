package heizoel.backend.domain.model;

import heizoel.backend.domain.model.enumeration.CommunicationChannel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
    private OrderSnapshot orderSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_channel", nullable = false, length = 20)
    private CommunicationChannel communicationChannel;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "delivery_window_start", nullable = false)
    private LocalTime deliveryWindowStart;

    @Column(name = "delivery_window_end", nullable = false)
    private LocalTime deliveryWindowEnd;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "response_deadline_hours", nullable = false)
    private Integer responseDeadlineHours;

    public static ConfirmationRequest create(
            OrderSnapshot orderSnapshot,
            String token,
            CommunicationChannel communicationChannel,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            LocalTime deliveryWindowEnd,
            Instant sentAt,
            Instant expiresAt,
            Integer responseDeadlineHours
    ) {
        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.orderSnapshot = orderSnapshot;
        confirmationRequest.token = token;
        confirmationRequest.communicationChannel = communicationChannel;
        confirmationRequest.deliveryDate = deliveryDate;
        confirmationRequest.deliveryWindowStart = deliveryWindowStart;
        confirmationRequest.deliveryWindowEnd = deliveryWindowEnd;
        confirmationRequest.active = true;
        confirmationRequest.sentAt = sentAt;
        confirmationRequest.expiresAt = expiresAt;
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
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            LocalTime deliveryWindowEnd,
            CommunicationChannel communicationChannel,
            Integer responseDeadlineHours
    ) {
        return this.deliveryDate.equals(deliveryDate)
                && this.deliveryWindowStart.equals(deliveryWindowStart)
                && this.deliveryWindowEnd.equals(deliveryWindowEnd)
                && this.communicationChannel == communicationChannel
                && this.responseDeadlineHours.equals(responseDeadlineHours);
    }
}

