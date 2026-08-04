package heizoel.backend.dashboard.adapter.persistence;

import heizoel.backend.adapter.out.persistence.ConfirmationOverviewQueryAdapter;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.application.model.overview.ConfirmationOverviewItem;
import heizoel.backend.application.port.out.persistence.ConfirmationOverviewFilter;
import heizoel.backend.configuration.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConfirmationOverviewQueryAdapter.class, QueryDslConfig.class})
class ConfirmationOverviewItemQueryAdapterIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
    private static final Instant SENT_AT = Instant.parse("2026-06-30T10:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_dashboard_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    ConfirmationOverviewQueryAdapter adapter;

    @Autowired
    EntityManager entityManager;

    @Test
    void findDashboardOrders_returnsOnlyOrdersBelongingToRequestedCompany() {
        Company requestedCompany = company("Requested tenant");
        Company otherCompany = company("Other tenant");
        order(requestedCompany, "SHARED-ORDER", "Visible Customer", "Visible Street",
                TODAY, ConfirmationStatus.SENT);
        order(otherCompany, "SHARED-ORDER", "Hidden Customer", "Hidden Street",
                TODAY, ConfirmationStatus.SENT);

        Page<ConfirmationOverviewItem> result = find(requestedCompany, null, null, null, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(ConfirmationOverviewItem::customerName)
                .containsExactly("Visible Customer");
    }

    @Test
    void findDashboardOrders_appliesDeliveryDateAndStatusAsCombinedFilters() {
        Company company = company("Date and status tenant");
        order(company, "MATCH", "Match", "One Street",
                TODAY.plusDays(1), ConfirmationStatus.CONFIRMED);
        order(company, "WRONG-STATUS", "Wrong Status", "Two Street",
                TODAY.plusDays(1), ConfirmationStatus.SENT);
        order(company, "WRONG-DATE", "Wrong Date", "Three Street",
                TODAY.plusDays(2), ConfirmationStatus.CONFIRMED);

        Page<ConfirmationOverviewItem> result = find(
                company,
                TODAY.plusDays(1),
                ConfirmationStatus.CONFIRMED,
                null,
                0,
                20
        );

        assertThat(result.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("MATCH");
    }

    @Test
    void findDashboardOrders_withExplicitDeliveryDateCanReturnPastNonProblemOrder() {
        Company company = company("Historical tenant");
        order(company, "PAST-CONFIRMED", "Past Customer", "Past Street",
                TODAY.minusDays(5), ConfirmationStatus.CONFIRMED);

        Page<ConfirmationOverviewItem> result = find(
                company,
                TODAY.minusDays(5),
                ConfirmationStatus.CONFIRMED,
                null,
                0,
                20
        );

        assertThat(result.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("PAST-CONFIRMED");
    }

    @Test
    void findDashboardOrders_withoutDateIncludesFutureOrdersAndPastProblemOrdersOnly() {
        Company company = company("Dashboard scope tenant");
        order(company, "FUTURE-SENT", "Future", "Future Street",
                TODAY.plusDays(1), ConfirmationStatus.SENT);
        order(company, "PAST-REJECTED", "Rejected", "Rejected Street",
                TODAY.minusDays(1), ConfirmationStatus.REJECTED);
        order(company, "PAST-NO-RESPONSE", "No Response", "No Response Street",
                TODAY.minusDays(2), ConfirmationStatus.NO_RESPONSE);
        order(company, "PAST-CONFIRMED", "Confirmed", "Confirmed Street",
                TODAY.minusDays(3), ConfirmationStatus.CONFIRMED);
        order(company, "PAST-SENT", "Sent", "Sent Street",
                TODAY.minusDays(4), ConfirmationStatus.SENT);

        Page<ConfirmationOverviewItem> result = find(company, null, null, null, 0, 20);

        assertThat(result.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("PAST-NO-RESPONSE", "PAST-REJECTED", "FUTURE-SENT");
    }

    @Test
    void findDashboardOrders_searchesExternalOrderIdAndCustomerNameCaseInsensitively() {
        Company company = company("Search tenant");
        order(company, "SPECIAL-4711", "Ordinary Customer", "Ordinary Street",
                TODAY, ConfirmationStatus.SENT);
        order(company, "ORDER-2", "Alice Wonderland", "Second Street",
                TODAY.plusDays(1), ConfirmationStatus.SENT);
        order(company, "ORDER-3", "Bob", "Berliner Allee 42",
                TODAY.plusDays(2), ConfirmationStatus.SENT);
        order(company, "ORDER-4", "Unrelated", "Other Road",
                TODAY.plusDays(3), ConfirmationStatus.SENT);

        assertThat(find(company, null, null, "special-47", 0, 20).getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("SPECIAL-4711");
        assertThat(find(company, null, null, "ALICE", 0, 20).getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("ORDER-2");
        assertThat(find(company, null, null, "allee 42", 0, 20).getContent())
                .isEmpty();
        assertThat(find(company, null, null, "does-not-exist", 0, 20).getContent())
                .isEmpty();
    }

    @Test
    void findDashboardOrders_returnsLatestConfirmationRequestOnly() {
        Company company = company("Latest request tenant");
        Order orderEntity = orderEntity(
                company,
                "RESENT-ORDER",
                "Resent Customer",
                "Resent Street",
                ConfirmationStatus.SENT
        );
        confirmationRequest(
                orderEntity,
                TODAY.plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                CommunicationChannel.EMAIL
        );
        confirmationRequest(
                orderEntity,
                TODAY.plusDays(3),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                CommunicationChannel.SMS
        );

        Page<ConfirmationOverviewItem> result = find(company, null, null, null, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(row -> {
            assertThat(row.deliveryDate()).isEqualTo(TODAY.plusDays(3));
            assertThat(row.deliveryWindowStart()).isEqualTo(LocalTime.of(14, 0));
            assertThat(row.deliveryWindowEnd()).isEqualTo(LocalTime.of(16, 0));
            assertThat(row.communicationChannel()).isEqualTo(CommunicationChannel.SMS);
        });
    }

    @Test
    void findDashboardOrders_paginatesInStableBusinessOrderAndReportsMetadata() {
        Company company = company("Pagination tenant");
        order(company, "ORDER-C", "Customer C", "Street C",
                TODAY.plusDays(2), ConfirmationStatus.SENT);
        order(company, "ORDER-B", "Customer B", "Street B",
                TODAY.plusDays(1), LocalTime.of(11, 0), ConfirmationStatus.SENT);
        order(company, "ORDER-A", "Customer A", "Street A",
                TODAY.plusDays(1), LocalTime.of(9, 0), ConfirmationStatus.SENT);
        order(company, "ORDER-D", "Customer D", "Street D",
                TODAY.plusDays(3), ConfirmationStatus.SENT);
        order(company, "ORDER-E", "Customer E", "Street E",
                TODAY.plusDays(4), ConfirmationStatus.SENT);

        Page<ConfirmationOverviewItem> firstPage = find(company, null, null, null, 0, 2);
        Page<ConfirmationOverviewItem> secondPage = find(company, null, null, null, 1, 2);
        Page<ConfirmationOverviewItem> lastPage = find(company, null, null, null, 2, 2);
        Page<ConfirmationOverviewItem> beyondLastPage = find(company, null, null, null, 3, 2);

        assertThat(firstPage.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("ORDER-A", "ORDER-B");
        assertThat(secondPage.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("ORDER-C", "ORDER-D");
        assertThat(lastPage.getContent())
                .extracting(ConfirmationOverviewItem::externalOrderId)
                .containsExactly("ORDER-E");
        assertThat(beyondLastPage.getContent()).isEmpty();
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(lastPage.isLast()).isTrue();
    }

    @Test
    void findDashboardOrders_returnsEmptyPageWithZeroTotalWhenNothingMatches() {
        Company company = company("Empty result tenant");

        Page<ConfirmationOverviewItem> result = find(
                company,
                TODAY,
                ConfirmationStatus.REJECTED,
                "missing",
                0,
                10
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    private Page<ConfirmationOverviewItem> find(
            Company company,
            LocalDate deliveryDate,
            ConfirmationStatus status,
            String search,
            int page,
            int size
    ) {
        entityManager.flush();
        entityManager.clear();

        return adapter.findOverview(
                new ConfirmationOverviewFilter(
                        company.getId(),
                        TODAY,
                        deliveryDate,
                        status,
                        search
                ),
                PageRequest.of(page, size)
        );
    }

    private Company company(String name) {
        Company company = Company.create(
                name + "-" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "http://dispo.example.test/callback"
        );
        entityManager.persist(company);
        return company;
    }

    private void order(
            Company company,
            String externalOrderId,
            String customerName,
            String address,
            LocalDate deliveryDate,
            ConfirmationStatus status
    ) {
        order(
                company,
                externalOrderId,
                customerName,
                address,
                deliveryDate,
                LocalTime.of(10, 0),
                status
        );
    }

    private void order(
            Company company,
            String externalOrderId,
            String customerName,
            String address,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            ConfirmationStatus status
    ) {
        Order orderEntity = orderEntity(
                company,
                externalOrderId,
                customerName,
                address,
                status
        );
        confirmationRequest(
                orderEntity,
                deliveryDate,
                deliveryWindowStart,
                deliveryWindowStart.plusHours(1),
                CommunicationChannel.EMAIL
        );
    }

    private Order orderEntity(
            Company company,
            String externalOrderId,
            String customerName,
            String address,
            ConfirmationStatus status
    ) {
        Order orderEntity = Order.create(
                company,
                externalOrderId,
                customerName,
                "customer@example.test",
                "+49123456789",
                address,
                "Heizöl",
                2_500,
                "2.500 EUR"
        );
        setStatus(orderEntity, status);
        entityManager.persist(orderEntity);
        return orderEntity;
    }

    private void confirmationRequest(
            Order orderEntity,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            LocalTime deliveryWindowEnd,
            CommunicationChannel channel
    ) {
        entityManager.persist(ConfirmationRequest.create(
                orderEntity,
                UUID.randomUUID().toString(),
                channel,
                DeliverySlot.of(
                        deliveryDate,
                        deliveryWindowStart,
                        deliveryWindowEnd
                ),
                SENT_AT,
                24
        ));
        entityManager.flush();
    }

    private void setStatus(Order orderEntity, ConfirmationStatus status) {
        switch (status) {
            case SENT -> orderEntity.markSent();
            case CONFIRMED -> orderEntity.markConfirmed();
            case REJECTED -> orderEntity.markRejected();
            case NO_RESPONSE -> orderEntity.markNoResponse();
        }
    }
}
