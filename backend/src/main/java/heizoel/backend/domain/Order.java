package heizoel.backend.domain;

import heizoel.backend.domain.company.Company;
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
                        name = "uk_order_snapshot_company_external_order_id",
                        columnNames = {"company_id", "external_order_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_snapshot_company_id",
                        columnList = "company_id"
                ),
                @Index(
                        name = "idx_order_snapshot_company_tour_number",
                        columnList = "company_id, tour_number"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Embedded
    private Tour tour;

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

    public static Order create(
            Company company,
            String externalOrderId,
            Tour tour,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        Order order = new Order();
        order.company = company;
        order.externalOrderId = externalOrderId;
        order.tour = tour;
        order.customerName = customerName;
        order.customerEmail = customerEmail;
        order.customerPhoneNumber = customerPhoneNumber;
        order.deliveryAddress = deliveryAddress;
        order.product = product;
        order.quantityLiters = quantityLiters;
        order.priceDisplayText = priceDisplayText;
        order.confirmationStatus = ConfirmationStatus.SENT;
        return order;
    }

    public void update(
            Tour tour,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        this.tour = tour;
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
            Tour tour,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText
    ) {
        return Objects.equals(this.tour, tour)
                && Objects.equals(this.customerName, customerName)
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