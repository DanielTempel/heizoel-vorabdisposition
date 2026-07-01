package heizoel.backend.confirmation.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "order_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_snapshot_external_order_id",
                        columnNames = "external_order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class OrderSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_order_id", nullable = false, unique = true, length = 100)
    private String externalOrderId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone_number", length = 50)
    private String customerPhoneNumber;

    @Column(name = "delivery_address", nullable = false, length = 1000)
    private String deliveryAddress;

    @Column(name = "product", nullable = false)
    private String product;

    @Column(name = "quantity_liters", nullable = false)
    private Integer quantityLiters;

    @Column(name = "price_display_text", length = 100)
    private String priceDisplayText;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false)
    private ConfirmationStatus confirmationStatus;

}
