package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConfirmationRequestRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_confirmation_request_repository_test")
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
    ConfirmationRequestRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void returnsRequestWhenItIsLatestForOrder() {
        Order order = createOrder("ORDER-SINGLE");
        ConfirmationRequest request = createRequest(order, "token-A");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestByToken("token-A"))
                .get()
                .extracting(ConfirmationRequest::getId)
                .isEqualTo(request.getId());
    }

    @Test
    void rejectsOlderTokenAndReturnsLatestTokenForSameOrder() {
        Order order = createOrder("ORDER-TWO-REQUESTS");
        createRequest(order, "token-A");
        ConfirmationRequest latest = createRequest(order, "token-B");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestByToken("token-A")).isEmpty();
        assertThat(repository.findLatestByToken("token-B"))
                .get()
                .extracting(ConfirmationRequest::getId)
                .isEqualTo(latest.getId());
    }

    @Test
    void calculatesLatestRequestWithinEachOrder() {
        Order orderA = createOrder("ORDER-A");
        Order orderB = createOrder("ORDER-B");
        createRequest(orderA, "token-A-1");
        ConfirmationRequest orderBRequest = createRequest(orderB, "token-B-1");
        createRequest(orderA, "token-A-2");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestByToken("token-B-1"))
                .get()
                .extracting(ConfirmationRequest::getId)
                .isEqualTo(orderBRequest.getId());
    }

    private Order createOrder(String externalOrderId) {
        String uniquePart = UUID.randomUUID().toString();
        Company company = Company.create(
                "Company " + uniquePart,
                "api-key-" + uniquePart,
                "http://dispo.example.test/callback"
        );
        entityManager.persist(company);

        Order order = Order.create(
                company,
                externalOrderId,
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                "customer@example.test",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        );
        entityManager.persist(order);
        return order;
    }

    private ConfirmationRequest createRequest(Order order, String token) {
        ConfirmationRequest request = ConfirmationRequest.createPending(
                order,
                token,
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2099, 6, 12),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                24
        );
        entityManager.persist(request);
        return request;
    }
}
