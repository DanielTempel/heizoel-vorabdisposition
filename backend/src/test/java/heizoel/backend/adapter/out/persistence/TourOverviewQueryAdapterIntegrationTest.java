package heizoel.backend.adapter.out.persistence;

import heizoel.backend.application.model.overview.OrderOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.application.port.out.persistence.TourOverviewFilter;
import heizoel.backend.configuration.QueryDslConfig;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TourOverviewQueryAdapter.class, QueryDslConfig.class})
class TourOverviewQueryAdapterIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
    private static final String LICENSE_PLATE = "WÜ-DEMO 100";

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
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    TourOverviewQueryAdapter adapter;

    @Autowired
    TestEntityManager entityManager;

    DashboardTestData testData;

    @BeforeEach
    void setUp() {
        testData = new DashboardTestData(entityManager);
    }

    @Test
    void filtersDeliveryDatesInclusively() {
        Company company = testData.createCompany("Date filter");
        createOrderWithRequest(company, "OLD", "TOUR-OLD", TODAY.minusDays(1), ConfirmationStatus.SENT);
        createOrderWithRequest(company, "TODAY", "TOUR-TODAY", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "TOMORROW", "TOUR-TOMORROW", TODAY.plusDays(1), ConfirmationStatus.SENT);

        Page<TourOverviewItem> fromToday = find(
                company, Set.of(), null, TODAY, null, 0, 20
        );
        Page<TourOverviewItem> exactToday = find(
                company, Set.of(), null, TODAY, TODAY, 0, 20
        );

        assertThat(fromToday.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-TODAY", "TOUR-TOMORROW");
        assertThat(exactToday.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-TODAY");
    }

    @Test
    void sortsToursByDeliveryDateThenTourNumber() {
        Company company = testData.createCompany("Tour sorting");
        createOrderWithRequest(company, "B", "TOUR-B", TODAY.plusDays(1), ConfirmationStatus.SENT);
        createOrderWithRequest(company, "C", "TOUR-C", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "A", "TOUR-A", TODAY, ConfirmationStatus.SENT);

        Page<TourOverviewItem> result = find(
                company, Set.of(), null, TODAY, null, 0, 20
        );

        assertThat(result.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-A", "TOUR-C", "TOUR-B");
    }

    @Test
    void groupsOrdersByUniqueTourNumber() {
        Company company = testData.createCompany("Tour grouping");
        createOrderWithRequest(company, "ORDER-A", "A-17", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "ORDER-B", "A-17", TODAY, ConfirmationStatus.CONFIRMED);

        Page<TourOverviewItem> result = find(
                company, Set.of(), null, TODAY, null, 0, 20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour -> {
            assertThat(tour.tourNumber()).isEqualTo("A-17");
            assertThat(tour.vehicleLicensePlate()).isEqualTo(LICENSE_PLATE);
            assertThat(tour.deliveryDate()).isEqualTo(TODAY);
            assertThat(tour.orders())
                    .extracting(OrderOverviewItem::externalOrderId)
                    .containsExactly("ORDER-A", "ORDER-B");
        });

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filtersToursAndOrdersByOneStatus() {
        Company company = testData.createCompany("Single status");
        createOrderWithRequest(company, "A-CONFIRMED", "TOUR-A", TODAY, ConfirmationStatus.CONFIRMED);
        createOrderWithRequest(company, "A-REJECTED", "TOUR-A", TODAY, ConfirmationStatus.REJECTED);
        createOrderWithRequest(company, "B-SENT", "TOUR-B", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "B-CONFIRMED", "TOUR-B", TODAY, ConfirmationStatus.CONFIRMED);

        Page<TourOverviewItem> result = find(
                company,
                Set.of(ConfirmationStatus.REJECTED),
                null,
                TODAY,
                null,
                0,
                20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour -> {
            assertThat(tour.tourNumber()).isEqualTo("TOUR-A");
            assertThat(tour.orders())
                    .extracting(OrderOverviewItem::externalOrderId)
                    .containsExactly("A-REJECTED");
        });
    }

    @Test
    void filtersToursAndOrdersBySeveralStatuses() {
        Company company = testData.createCompany("Several statuses");
        createOrderWithRequest(company, "SENT", "TOUR-A", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "CONFIRMED", "TOUR-A", TODAY, ConfirmationStatus.CONFIRMED);
        createOrderWithRequest(company, "REJECTED", "TOUR-A", TODAY, ConfirmationStatus.REJECTED);
        createOrderWithRequest(company, "NO-RESPONSE", "TOUR-A", TODAY, ConfirmationStatus.NO_RESPONSE);
        createOrderWithRequest(company, "OTHER-SENT", "TOUR-B", TODAY, ConfirmationStatus.SENT);

        Page<TourOverviewItem> result = find(
                company,
                Set.of(ConfirmationStatus.REJECTED, ConfirmationStatus.NO_RESPONSE),
                null,
                TODAY,
                null,
                0,
                20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour ->
                assertThat(tour.orders())
                        .extracting(OrderOverviewItem::confirmationStatus)
                        .containsExactlyInAnyOrder(
                                ConfirmationStatus.REJECTED,
                                ConfirmationStatus.NO_RESPONSE
                        )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"A-17", "WÜ-DEMO 100"})
    void searchByTourFieldsReturnsAllTourOrders(String search) {
        Company company = testData.createCompany("Tour search");
        createOrderWithRequest(company, "ORDER-A", "A-17", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(company, "ORDER-B", "A-17", TODAY, ConfirmationStatus.CONFIRMED);
        createOrderWithRequest(company, "ORDER-C", "A-17", TODAY, ConfirmationStatus.REJECTED);

        Page<TourOverviewItem> result = find(
                company, Set.of(), search, TODAY, null, 0, 20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour ->
                assertThat(tour.orders())
                        .extracting(OrderOverviewItem::externalOrderId)
                        .containsExactly("ORDER-A", "ORDER-B", "ORDER-C")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "ORDER-A, ORDER-A",
            "Schmidt, ORDER-B",
            "Gamma-Allee, ORDER-C"
    })
    void searchByOrderFieldsReturnsOnlyMatchingOrders(
            String search,
            String expectedExternalOrderId
    ) {
        Company company = testData.createCompany("Order search");
        createOrderWithRequest(
                company, "ORDER-A", "A-17", "Müller", "Alpha-Straße", TODAY, ConfirmationStatus.SENT
        );
        createOrderWithRequest(
                company, "ORDER-B", "A-17", "Schmidt", "Beta-Straße", TODAY, ConfirmationStatus.SENT
        );
        createOrderWithRequest(
                company, "ORDER-C", "A-17", "Weber", "Gamma-Allee", TODAY, ConfirmationStatus.SENT
        );

        Page<TourOverviewItem> result = find(
                company, Set.of(), search, TODAY, null, 0, 20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour ->
                assertThat(tour.orders())
                        .extracting(OrderOverviewItem::externalOrderId)
                        .containsExactly(expectedExternalOrderId)
        );
    }

    @Test
    void searchesCaseInsensitively() {
        Company company = testData.createCompany("Case insensitive search");
        createOrderWithRequest(
                company,
                "ORDER-A",
                "A-17",
                "Max Müller",
                "Alpha-Straße",
                TODAY,
                ConfirmationStatus.SENT
        );

        Page<TourOverviewItem> result = find(
                company, Set.of(), "mÜLlEr", TODAY, null, 0, 20
        );

        assertThat(orders(result))
                .extracting(OrderOverviewItem::externalOrderId)
                .containsExactly("ORDER-A");
    }

    @Test
    void combinesTourSearchWithStatusFilter() {
        Company company = testData.createCompany("Combined filters");
        createOrderWithRequest(company, "MULLER", "A-17", "Müller", "One", TODAY, ConfirmationStatus.CONFIRMED);
        createOrderWithRequest(company, "SCHMIDT", "A-17", "Schmidt", "Two", TODAY, ConfirmationStatus.REJECTED);
        createOrderWithRequest(company, "WEBER", "A-17", "Weber", "Three", TODAY, ConfirmationStatus.REJECTED);

        Page<TourOverviewItem> result = find(
                company,
                Set.of(ConfirmationStatus.REJECTED),
                LICENSE_PLATE,
                TODAY,
                null,
                0,
                20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour ->
                assertThat(tour.orders())
                        .extracting(OrderOverviewItem::externalOrderId)
                        .containsExactly("SCHMIDT", "WEBER")
        );
    }

    @Test
    void usesOnlyLatestConfirmationRequest() {
        Company company = testData.createCompany("Latest request");
        Order order = testData.createOrder(
                company,
                "ORDER-A",
                "A-17",
                LICENSE_PLATE,
                ConfirmationStatus.SENT
        );
        testData.createRequest(
                order,
                TODAY.minusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                CommunicationChannel.EMAIL
        );
        testData.createRequest(
                order,
                TODAY,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                CommunicationChannel.SMS
        );

        Page<TourOverviewItem> result = find(
                company, Set.of(), null, TODAY, TODAY, 0, 20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour -> {
            assertThat(tour.deliveryDate()).isEqualTo(TODAY);
            assertThat(tour.orders()).singleElement().satisfies(item -> {
                assertThat(item.deliveryWindowStart()).isEqualTo(LocalTime.of(8, 0));
                assertThat(item.deliveryWindowEnd()).isEqualTo(LocalTime.of(9, 0));
                assertThat(item.communicationChannel()).isEqualTo(CommunicationChannel.SMS);
            });
        });
    }

    @Test
    void sortsOrdersInsideTourByWindowAndExternalOrderId() {
        Company company = testData.createCompany("Order sorting");
        createOrderWithRequest(
                company, "ORDER-B", "A-17", TODAY, LocalTime.of(8, 0), LocalTime.of(9, 0), ConfirmationStatus.SENT
        );
        createOrderWithRequest(
                company, "ORDER-A", "A-17", TODAY, LocalTime.of(8, 0), LocalTime.of(9, 0), ConfirmationStatus.SENT
        );
        createOrderWithRequest(
                company, "ORDER-C", "A-17", TODAY, LocalTime.of(8, 0), LocalTime.of(10, 0), ConfirmationStatus.SENT
        );
        createOrderWithRequest(
                company, "ORDER-D", "A-17", TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0), ConfirmationStatus.SENT
        );

        Page<TourOverviewItem> result = find(
                company, Set.of(), null, TODAY, null, 0, 20
        );

        assertThat(result.getContent()).singleElement().satisfies(tour ->
                assertThat(tour.orders())
                        .extracting(OrderOverviewItem::externalOrderId)
                        .containsExactly("ORDER-A", "ORDER-B", "ORDER-C", "ORDER-D")
        );
    }

    @Test
    void paginatesByTourWithoutSplittingOrders() {
        Company company = testData.createCompany("Tour pagination");
        createTourOrders(company, "TOUR-A", 5);
        createTourOrders(company, "TOUR-B", 3);
        createTourOrders(company, "TOUR-C", 4);

        Page<TourOverviewItem> firstPage = find(
                company, Set.of(), null, TODAY, null, 0, 2
        );
        Page<TourOverviewItem> secondPage = find(
                company, Set.of(), null, TODAY, null, 1, 2
        );

        assertThat(firstPage.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-A", "TOUR-B");
        assertThat(firstPage.getContent().get(0).orders()).hasSize(5);
        assertThat(secondPage.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-C");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void isolatesCompanyData() {
        Company companyA = testData.createCompany("Company A");
        Company companyB = testData.createCompany("Company B");
        createOrderWithRequest(companyA, "ORDER-A", "TOUR-A", TODAY, ConfirmationStatus.SENT);
        createOrderWithRequest(companyB, "ORDER-B", "TOUR-B", TODAY, ConfirmationStatus.SENT);

        Page<TourOverviewItem> result = find(
                companyA, Set.of(), null, TODAY, null, 0, 20
        );

        assertThat(result.getContent())
                .extracting(TourOverviewItem::tourNumber)
                .containsExactly("TOUR-A");
        assertThat(orders(result))
                .extracting(OrderOverviewItem::externalOrderId)
                .containsExactly("ORDER-A");
    }

    @Test
    void returnsEmptyPageWhenNothingMatches() {
        Company company = testData.createCompany("Empty result");
        createOrderWithRequest(company, "ORDER-A", "TOUR-A", TODAY, ConfirmationStatus.SENT);

        Page<TourOverviewItem> result = find(
                company,
                Set.of(ConfirmationStatus.REJECTED),
                "missing",
                TODAY,
                null,
                0,
                20
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    private Page<TourOverviewItem> find(
            Company company,
            Set<ConfirmationStatus> statuses,
            String search,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        testData.flushAndClear();
        return adapter.findTours(
                new TourOverviewFilter(
                        company.getId(),
                        statuses,
                        search,
                        dateFrom,
                        dateTo
                ),
                PageRequest.of(page, size)
        );
    }

    private void createOrderWithRequest(
            Company company,
            String externalOrderId,
            String tourNumber,
            LocalDate deliveryDate,
            ConfirmationStatus status
    ) {
        createOrderWithRequest(
                company,
                externalOrderId,
                tourNumber,
                "Customer " + externalOrderId,
                "Address " + externalOrderId,
                deliveryDate,
                status
        );
    }

    private void createOrderWithRequest(
            Company company,
            String externalOrderId,
            String tourNumber,
            String customerName,
            String deliveryAddress,
            LocalDate deliveryDate,
            ConfirmationStatus status
    ) {
        Order order = testData.createOrder(
                company,
                externalOrderId,
                tourNumber,
                LICENSE_PLATE,
                customerName,
                deliveryAddress,
                status
        );
        testData.createRequest(
                order,
                deliveryDate,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0)
        );
    }

    private void createOrderWithRequest(
            Company company,
            String externalOrderId,
            String tourNumber,
            LocalDate deliveryDate,
            LocalTime start,
            LocalTime end,
            ConfirmationStatus status
    ) {
        Order order = testData.createOrder(
                company,
                externalOrderId,
                tourNumber,
                LICENSE_PLATE,
                status
        );
        testData.createRequest(order, deliveryDate, start, end);
    }

    private void createTourOrders(
            Company company,
            String tourNumber,
            int orderCount
    ) {
        IntStream.rangeClosed(1, orderCount).forEach(index ->
                createOrderWithRequest(
                        company,
                        tourNumber + "-ORDER-" + index,
                        tourNumber,
                        TODAY,
                        ConfirmationStatus.SENT
                )
        );
    }

    private List<OrderOverviewItem> orders(Page<TourOverviewItem> result) {
        return result.getContent()
                .stream()
                .flatMap(tour -> tour.orders().stream())
                .toList();
    }
}
