package heizoel.backend.adapter.in.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/test/configure-security-test-company.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class DispoSecurityIntegrationTest {

    private static final String TEST_API_KEY = "security-tenant-api-key";
    private static final long TEST_COMPANY_ID = 1001L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("heizoel_backend_test")
            .withUsername("heizoel")
            .withPassword("heizoel");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @BeforeEach
    void cleanDatabase() {
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void allowsCorsPreflightWithoutApiKey() throws Exception {
        mockMvc.perform(options("/api/dispo/confirmation-requests")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-API-Key, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:3000"
                ));
    }

    @Test
    void rejectsDispoRequestWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {}
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }

    @Test
    void rejectsInvalidApiKey() throws Exception {
        mockMvc.perform(post("/api/dispo/confirmation-requests")
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request("ORDER-WRONG-KEY")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("Invalid API key."));
    }

    @Test
    void acceptsRequestWithValidApiKey() throws Exception {
        performCreate("ORDER-VALID-KEY")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId").value("ORDER-VALID-KEY"))
                .andExpect(jsonPath("$.confirmationStatus").value("OPEN"));
    }

    @Test
    void createsOrderForCompanyAuthenticatedByApiKey() throws Exception {
        performCreate("ORDER-TENANT")
                .andExpect(status().isAccepted());

        Order order = orderRepository.findAll().get(0);

        assertThat(order.getCompany().getId()).isEqualTo(TEST_COMPANY_ID);
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            String externalOrderId
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .header("X-API-Key", TEST_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        request(externalOrderId)
                )));
    }

    private DispoConfirmationRequestDto request(String externalOrderId) {
        return new DispoConfirmationRequestDto(
                externalOrderId,
                "SECURITY-TOUR",
                "WUE-SE 1001",
                "Security Test Customer",
                CommunicationChannel.EMAIL,
                "security-customer@example.com",
                null,
                "Security Street 1, 97070 Würzburg",
                "Heating oil",
                3_000,
                LocalDate.of(2099, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                24,
                "100 EUR"
        );
    }
}
