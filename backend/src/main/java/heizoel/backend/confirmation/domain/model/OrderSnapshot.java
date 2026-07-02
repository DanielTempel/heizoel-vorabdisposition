package heizoel.backend.confirmation.domain.model;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Objects;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_order_id", nullable = false, length = 100)
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

    public static OrderSnapshot create(
            String externalOrderId,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        OrderSnapshot orderSnapshot = new OrderSnapshot();
        orderSnapshot.externalOrderId = externalOrderId;
        orderSnapshot.customerName = customerName;
        orderSnapshot.customerEmail = customerEmail;
        orderSnapshot.customerPhoneNumber = customerPhoneNumber;
        orderSnapshot.deliveryAddress = deliveryAddress;
        orderSnapshot.product = product;
        orderSnapshot.quantityLiters = quantityLiters;
        orderSnapshot.priceDisplayText = priceDisplayText;
        orderSnapshot.confirmationStatus = ConfirmationStatus.SENT;
        return orderSnapshot;
    }

    public void update(
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhoneNumber = customerPhoneNumber;
        this.deliveryAddress = deliveryAddress;
        this.product = product;
        this.quantityLiters = quantityLiters;
        this.priceDisplayText = priceDisplayText;
        this.confirmationStatus = ConfirmationStatus.SENT;
    }

    public boolean hasSameData(
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        return Objects.equals(this.customerName, customerName)
                && Objects.equals(this.customerEmail, customerEmail)
                && Objects.equals(this.customerPhoneNumber, customerPhoneNumber)
                && Objects.equals(this.deliveryAddress, deliveryAddress)
                && Objects.equals(this.product, product)
                && Objects.equals(this.quantityLiters, quantityLiters)
                && Objects.equals(this.priceDisplayText, priceDisplayText);
    }

    public void markSent() {
        this.confirmationStatus = ConfirmationStatus.SENT;
    }

    public void markConfirmed() {
        this.confirmationStatus = ConfirmationStatus.CONFIRMED;
    }

    public void markRejected() {
        this.confirmationStatus = ConfirmationStatus.REJECTED;
    }

    public void markNoResponse() {
        this.confirmationStatus = ConfirmationStatus.NO_RESPONSE;
    }
}
