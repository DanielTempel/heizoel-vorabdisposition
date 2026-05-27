package heizoel.backend.dispo.domain.entity;


import heizoel.backend.notification.domain.CommunicationChannel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
@NoArgsConstructor
public class ConfirmationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, length = 255)
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
}
