package heizoel.backend.adapter.out.persistence;

import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.RequestDetail;
import heizoel.backend.configuration.QueryDslConfig;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConfirmationDetailQueryAdapter.class, QueryDslConfig.class})
class ConfirmationDetailQueryAdapterIntegrationTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 8, 10);
    private static final LocalTime DELIVERY_START = LocalTime.of(8, 0);
    private static final LocalTime DELIVERY_END = LocalTime.of(10, 0);
    private static final Instant SENT_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T12:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_confirmation_detail_test")
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
    ConfirmationDetailQueryAdapter adapter;

    @Autowired
    TestEntityManager entityManager;

    DashboardTestData testData;

    @BeforeEach
    void setUp() {
        testData = new DashboardTestData(entityManager);
    }

    @Test
    void returnsActiveRequestWithoutResponseAsCurrentRequest() {
        Company company = testData.createCompany("Active request");
        Order order = testData.createOrder(
                company,
                "ORDER-ACTIVE",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.SENT
        );
        ConfirmationRequest request = createRequest(
                order,
                SENT_AT,
                true,
                CommunicationChannel.EMAIL,
                null,
                null
        );

        ConfirmationDetail result = find(company.getId(), "ORDER-ACTIVE").orElseThrow();

        assertThat(result.order()).satisfies(orderDetail -> {
            assertThat(orderDetail.externalOrderId()).isEqualTo("ORDER-ACTIVE");
            assertThat(orderDetail.customerName()).isEqualTo("Customer ORDER-ACTIVE");
            assertThat(orderDetail.customerEmail()).isEqualTo("customer@example.test");
            assertThat(orderDetail.customerPhoneNumber()).isEqualTo("+49123456789");
            assertThat(orderDetail.deliveryAddress()).isEqualTo("Address ORDER-ACTIVE");
            assertThat(orderDetail.product()).isEqualTo("Heizöl");
            assertThat(orderDetail.quantityLiters()).isEqualTo(2_500);
            assertThat(orderDetail.priceDisplayText()).isEqualTo("2.500 EUR");
            assertThat(orderDetail.tourNumber()).isEqualTo("A-17");
            assertThat(orderDetail.vehicleLicensePlate()).isEqualTo("WUE-DEMO 100");
            assertThat(orderDetail.confirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        });
        assertThat(result.currentRequest()).satisfies(current -> {
            assertThat(current.requestId()).isEqualTo(request.getId());
            assertThat(current.communicationChannel()).isEqualTo(CommunicationChannel.EMAIL);
            assertThat(current.deliveryDate()).isEqualTo(DELIVERY_DATE);
            assertThat(current.deliveryWindowStart()).isEqualTo(DELIVERY_START);
            assertThat(current.deliveryWindowEnd()).isEqualTo(DELIVERY_END);
            assertThat(current.sentAt()).isEqualTo(SENT_AT);
            assertThat(current.expiresAt()).isEqualTo(SENT_AT.plusSeconds(24 * 60 * 60));
            assertThat(current.responseDeadlineHours()).isEqualTo(24);
            assertThat(current.active()).isTrue();
            assertThat(current.status()).isEqualTo(ConfirmationStatus.SENT);
            assertThat(current.customerResponse()).isNull();
        });
        assertThat(result.previousRequests()).isEmpty();
    }

    @Test
    void returnsNewestRequestAsCurrentAndOlderRequestsInDescendingOrder() {
        Company company = testData.createCompany("Request history");
        Order order = testData.createOrder(
                company,
                "ORDER-HISTORY",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.SENT
        );
        ConfirmationRequest first = createRequest(
                order,
                SENT_AT,
                false,
                CommunicationChannel.EMAIL,
                null,
                null
        );
        ConfirmationRequest second = createRequest(
                order,
                SENT_AT.plusSeconds(3_600),
                false,
                CommunicationChannel.SMS,
                CustomerResponseType.REJECT,
                "Please deliver another day"
        );
        ConfirmationRequest third = createRequest(
                order,
                SENT_AT.plusSeconds(7_200),
                true,
                CommunicationChannel.EMAIL,
                null,
                null
        );

        ConfirmationDetail result = find(company.getId(), "ORDER-HISTORY").orElseThrow();

        assertThat(result.currentRequest().requestId()).isEqualTo(third.getId());
        assertThat(result.currentRequest().status()).isEqualTo(ConfirmationStatus.SENT);
        assertThat(result.previousRequests())
                .extracting(RequestDetail::requestId)
                .containsExactly(second.getId(), first.getId());
        assertThat(result.previousRequests())
                .extracting(RequestDetail::status)
                .containsExactly(ConfirmationStatus.REJECTED, ConfirmationStatus.NO_RESPONSE);
    }

    @Test
    void mapsCustomerResponseFields() {
        Company company = testData.createCompany("Customer response");
        Order order = testData.createOrder(
                company,
                "ORDER-RESPONSE",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.CONFIRMED
        );
        createRequest(
                order,
                SENT_AT,
                false,
                CommunicationChannel.WHATSAPP,
                CustomerResponseType.CONFIRM,
                "Delivery is fine"
        );

        ConfirmationDetail result = find(company.getId(), "ORDER-RESPONSE").orElseThrow();

        assertThat(result.currentRequest().status()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(result.currentRequest().customerResponse()).satisfies(response -> {
            assertThat(response.responseType()).isEqualTo(CustomerResponseType.CONFIRM);
            assertThat(response.comment()).isEqualTo("Delivery is fine");
            assertThat(response.receivedAt()).isEqualTo(RECEIVED_AT);
        });
    }

    @Test
    void mapsInactiveRequestWithoutResponseAsNoResponse() {
        Company company = testData.createCompany("No response");
        Order order = testData.createOrder(
                company,
                "ORDER-NO-RESPONSE",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.NO_RESPONSE
        );
        createRequest(
                order,
                SENT_AT,
                false,
                CommunicationChannel.EMAIL,
                null,
                null
        );

        ConfirmationDetail result = find(company.getId(), "ORDER-NO-RESPONSE").orElseThrow();

        assertThat(result.currentRequest().status()).isEqualTo(ConfirmationStatus.NO_RESPONSE);
        assertThat(result.currentRequest().customerResponse()).isNull();
    }

    @Test
    void isolatesOrdersByCompany() {
        Company companyA = testData.createCompany("Company A");
        Company companyB = testData.createCompany("Company B");
        Company companyWithoutOrder = testData.createCompany("Company without order");
        testData.createOrder(
                companyA,
                "SHARED-ORDER",
                "A-17",
                "WUE-A 100",
                "Customer A",
                "Address A",
                ConfirmationStatus.SENT
        );
        testData.createOrder(
                companyB,
                "SHARED-ORDER",
                "B-20",
                "WUE-B 200",
                "Customer B",
                "Address B",
                ConfirmationStatus.SENT
        );

        Optional<ConfirmationDetail> resultA = find(companyA.getId(), "SHARED-ORDER");
        Optional<ConfirmationDetail> resultB = adapter.findDetail(companyB.getId(), "SHARED-ORDER");
        Optional<ConfirmationDetail> unknownCompanyResult = adapter.findDetail(
                companyWithoutOrder.getId(),
                "SHARED-ORDER"
        );

        assertThat(resultA).get().extracting(detail -> detail.order().customerName())
                .isEqualTo("Customer A");
        assertThat(resultB).get().extracting(detail -> detail.order().customerName())
                .isEqualTo("Customer B");
        assertThat(unknownCompanyResult).isEmpty();
    }

    @Test
    void returnsEmptyWhenExternalOrderIdIsUnknown() {
        Company company = testData.createCompany("Unknown order");

        Optional<ConfirmationDetail> result = find(company.getId(), "MISSING");

        assertThat(result).isEmpty();
    }

    @Test
    void usesRequestIdAsTieBreakerWhenSentAtIsEqual() {
        Company company = testData.createCompany("Stable sorting");
        Order order = testData.createOrder(
                company,
                "ORDER-SORTING",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.SENT
        );
        ConfirmationRequest lowerId = createRequest(
                order,
                SENT_AT,
                false,
                CommunicationChannel.EMAIL,
                null,
                null
        );
        ConfirmationRequest higherId = createRequest(
                order,
                SENT_AT,
                true,
                CommunicationChannel.SMS,
                null,
                null
        );

        ConfirmationDetail result = find(company.getId(), "ORDER-SORTING").orElseThrow();

        assertThat(higherId.getId()).isGreaterThan(lowerId.getId());
        assertThat(result.currentRequest().requestId()).isEqualTo(higherId.getId());
        assertThat(result.previousRequests())
                .extracting(RequestDetail::requestId)
                .containsExactly(lowerId.getId());
    }

    @Test
    void returnsOrderWithoutRequests() {
        Company company = testData.createCompany("Order without request");
        testData.createOrder(
                company,
                "ORDER-WITHOUT-REQUEST",
                "A-17",
                "WUE-DEMO 100",
                ConfirmationStatus.SENT
        );

        ConfirmationDetail result = find(
                company.getId(),
                "ORDER-WITHOUT-REQUEST"
        ).orElseThrow();

        assertThat(result.currentRequest()).isNull();
        assertThat(result.previousRequests()).isEmpty();
    }

    private Optional<ConfirmationDetail> find(
            Long companyId,
            String externalOrderId
    ) {
        testData.flushAndClear();
        return adapter.findDetail(companyId, externalOrderId);
    }

    private ConfirmationRequest createRequest(
            Order order,
            Instant sentAt,
            boolean active,
            CommunicationChannel channel,
            CustomerResponseType responseType,
            String comment
    ) {
        ConfirmationRequest request = ConfirmationRequest.create(
                order,
                UUID.randomUUID().toString(),
                channel,
                DeliverySlot.of(DELIVERY_DATE, DELIVERY_START, DELIVERY_END),
                sentAt,
                24
        );
        if (!active) {
            request.markInactive();
        }
        entityManager.persist(request);

        if (responseType != null) {
            entityManager.persist(CustomerResponse.create(
                    request,
                    responseType,
                    comment,
                    RECEIVED_AT
            ));
        }
        return request;
    }
}
