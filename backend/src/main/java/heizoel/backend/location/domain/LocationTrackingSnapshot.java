package heizoel.backend.location.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "location_tracking_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_location_tracking_snapshot_external_order_id",
                        columnNames = "external_order_id"
                ),
                @UniqueConstraint(
                        name = "uk_location_tracking_snapshot_confirmation_token",
                        columnNames = "confirmation_token"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LocationTrackingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "confirmation_token", length = 255)
    private String confirmationToken;

    @Column(name = "delivery_address", nullable = false, length = 1000)
    private String deliveryAddress;

    @Column(name = "location_x", nullable = false)
    private Double locationX;

    @Column(name = "location_y", nullable = false)
    private Double locationY;

    @Column(name = "target_location_x", nullable = false)
    private Double targetLocationX;

    @Column(name = "target_location_y", nullable = false)
    private Double targetLocationY;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
