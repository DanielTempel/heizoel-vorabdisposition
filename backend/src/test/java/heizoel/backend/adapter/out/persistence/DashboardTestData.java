package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

final class DashboardTestData {

    private static final Instant SENT_AT = Instant.parse("2026-07-01T10:00:00Z");

    private final TestEntityManager entityManager;

    DashboardTestData(TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    Company createCompany(String name) {
        String uniquePart = UUID.randomUUID().toString();
        Company company = Company.create(
                name + "-" + uniquePart,
                "api-key-" + uniquePart,
                "http://dispo.example.test/callback"
        );
        entityManager.persist(company);
        return company;
    }

    Order createOrder(
            Company company,
            String externalOrderId,
            String tourNumber,
            String vehicleLicensePlate,
            ConfirmationStatus status
    ) {
        return createOrder(
                company,
                externalOrderId,
                tourNumber,
                vehicleLicensePlate,
                "Customer " + externalOrderId,
                "Address " + externalOrderId,
                status
        );
    }

    Order createOrder(
            Company company,
            String externalOrderId,
            String tourNumber,
            String vehicleLicensePlate,
            String customerName,
            String deliveryAddress,
            ConfirmationStatus status
    ) {
        Order order = Order.create(
                company,
                externalOrderId,
                Tour.of(tourNumber, vehicleLicensePlate),
                customerName,
                "customer@example.test",
                "+49123456789",
                deliveryAddress,
                "Heizöl",
                2_500,
                "2.500 EUR"
        );
        setStatus(order, status);
        entityManager.persist(order);
        return order;
    }

    void createRequest(
            Order order,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
        createRequest(order, date, start, end, CommunicationChannel.EMAIL);
    }

    void createRequest(
            Order order,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            CommunicationChannel channel
    ) {
        ConfirmationRequest request = ConfirmationRequest.createPending(
                order,
                UUID.randomUUID().toString(),
                channel,
                DeliverySlot.of(date, start, end),
                24
        );
        request.markSent(SENT_AT);
        entityManager.persist(request);
    }

    void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void setStatus(Order order, ConfirmationStatus status) {
        switch (status) {
            case SENT -> order.markSent();
            case CONFIRMED -> order.markConfirmed();
            case REJECTED -> order.markRejected();
            case NO_RESPONSE -> order.markNoResponse();
        }
    }
}
