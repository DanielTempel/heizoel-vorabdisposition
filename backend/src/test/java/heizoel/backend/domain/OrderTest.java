package heizoel.backend.domain;

import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private static final Tour ORIGINAL_TOUR = Tour.of("17", "WÜ-AB 123");

    @Test
    void shouldHaveSameDataWhenTourValuesAreEqual() {
        Order order = order(ORIGINAL_TOUR);

        boolean sameData = hasSameData(order, Tour.of("17", "WÜ-AB 123"));

        assertThat(sameData).isTrue();
    }

    @Test
    void shouldDetectChangedTourNumber() {
        Order order = order(ORIGINAL_TOUR);

        boolean sameData = hasSameData(order, Tour.of("18", "WÜ-AB 123"));

        assertThat(sameData).isFalse();
    }

    @Test
    void shouldDetectChangedVehicleLicensePlate() {
        Order order = order(ORIGINAL_TOUR);

        boolean sameData = hasSameData(order, Tour.of("17", "WÜ-CD 456"));

        assertThat(sameData).isFalse();
    }

    @Test
    void shouldDetectEveryChangedOrderField() {
        Order order = order(ORIGINAL_TOUR);

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                "Changed Customer",
                "max@example.com",
                "+491701234567",
                order.getDeliveryAddress(),
                order.getProduct(),
                3000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "changed@example.com",
                "+491701234567",
                order.getDeliveryAddress(),
                order.getProduct(),
                3000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "max@example.com",
                "+491709999999",
                order.getDeliveryAddress(),
                order.getProduct(),
                3000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "max@example.com",
                "+491701234567",
                "Changed address",
                order.getProduct(),
                3000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "max@example.com",
                "+491701234567",
                order.getDeliveryAddress(),
                "Changed product",
                3000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "max@example.com",
                "+491701234567",
                order.getDeliveryAddress(),
                order.getProduct(),
                4000,
                order.getPriceDisplayText()
        )).isFalse();

        assertThat(order.hasSameData(
                ORIGINAL_TOUR,
                order.getCustomerName(),
                "max@example.com",
                "+491701234567",
                order.getDeliveryAddress(),
                order.getProduct(),
                3000,
                "Changed price"
        )).isFalse();
    }

    @Test
    void updateStoresNewDataWithoutChangingStatus() {
        Order order = order(ORIGINAL_TOUR);
        order.markRejected();
        Tour changedTour = Tour.of("18", "WÜ-CD 456");

        order.update(
                changedTour,
                "Changed Customer",
                "changed@example.com",
                "+491709999999",
                "Neue Straße 5",
                "Premium Heizöl",
                4000,
                "99,00 € / 100 L"
        );

        assertThat(order.getTour()).isEqualTo(changedTour);
        assertThat(order.getCustomerName()).isEqualTo("Changed Customer");
        assertThat(order.getCustomerEmail()).isEqualTo("changed@example.com");
        assertThat(order.getCustomerPhoneNumber()).isEqualTo("+491709999999");
        assertThat(order.getDeliveryAddress()).isEqualTo("Neue Straße 5");
        assertThat(order.getProduct()).isEqualTo("Premium Heizöl");
        assertThat(order.getQuantityLiters()).isEqualTo(4000);
        assertThat(order.getPriceDisplayText()).isEqualTo("99,00 € / 100 L");
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.REJECTED);
    }

    @Test
    void markOpenSetsConfirmationStatusToOpen() {
        Order order = createOrder();

        order.markOpen();

        assertThat(order.getConfirmationStatus())
                .isEqualTo(ConfirmationStatus.OPEN);
    }

    private Order createOrder() {
        return order(ORIGINAL_TOUR);
    }

    private Order order(Tour tour) {
        return Order.create(
                Company.create(
                        "Test Company",
                        "api-key-hash",
                        "http://localhost/callback"
                ),
                "A-1024",
                tour,
                "Max Mustermann",
                "max@example.com",
                "+491701234567",
                "Musterstraße 12",
                "Heizöl",
                3000,
                "95,40 € / 100 L"
        );
    }

    private boolean hasSameData(Order order, Tour tour) {
        return order.hasSameData(
                tour,
                "Max Mustermann",
                "max@example.com",
                "+491701234567",
                "Musterstraße 12",
                "Heizöl",
                3000,
                "95,40 € / 100 L"
        );
    }
}
