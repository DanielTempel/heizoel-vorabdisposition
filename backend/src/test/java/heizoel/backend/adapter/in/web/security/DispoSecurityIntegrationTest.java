package heizoel.backend.adapter.in.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.adapter.out.persistence.CompanyEmailSettingsRepository;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.company.CompanyEmailSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private static final String SECOND_TEST_API_KEY =
            "security-second-tenant-api-key";
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

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    CompanyEmailSettingsRepository companyEmailSettingsRepository;

    @Autowired
    ApiKeyHasher apiKeyHasher;

    Company secondCompany;

    @BeforeEach
    void cleanDatabase() {
        companyEmailSettingsRepository.deleteAll();
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();
        secondCompany = companyRepository
                .findByApiKeyHash(apiKeyHasher.hash(SECOND_TEST_API_KEY))
                .orElseGet(() -> companyRepository.save(Company.create(
                        "Second Security Test Company",
                        apiKeyHasher.hash(SECOND_TEST_API_KEY),
                        "http://localhost:8082/api/dispo/callback"
                )));
    }

    @Test
    void rejectsCorsPreflightForDispoApi() throws Exception {
        mockMvc.perform(options("/api/dispo/confirmation-requests")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-API-Key, Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        "Access-Control-Allow-Origin"
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

    @Test
    void rejectsEmailSettingsRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/dispo/settings/email"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }

    @Test
    void rejectsEmailSettingsRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/dispo/settings/email")
                        .header("X-API-Key", "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    @Test
    void acceptsEmailSettingsRequestWithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/dispo/settings/email")
                        .header("X-API-Key", TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void allowsPublicCustomerConfirmationRouteWithoutApiKey()
            throws Exception {
        mockMvc.perform(get(
                        "/api/customer/confirmations/{token}",
                        "unknown-token"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CONFIRMATION_REQUEST_NOT_FOUND"));
    }

    @Test
    void createsSameExternalOrderIdIndependentlyForBothCompanies()
            throws Exception {
        performCreate(
                TEST_API_KEY,
                request("SHARED-ORDER", "Customer A")
        ).andExpect(status().isAccepted());
        performCreate(
                SECOND_TEST_API_KEY,
                request("SHARED-ORDER", "Customer B")
        ).andExpect(status().isAccepted());

        assertThat(orderRepository.findAll())
                .filteredOn(order -> order.getExternalOrderId()
                        .equals("SHARED-ORDER"))
                .extracting(
                        order -> order.getCompany().getId(),
                        Order::getCustomerName
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                TEST_COMPANY_ID,
                                "Customer A"
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                secondCompany.getId(),
                                "Customer B"
                        )
                );
    }

    @Test
    void dashboardReadsSameExternalOrderIdOnlyFromSessionCompany()
            throws Exception {
        performCreate(
                TEST_API_KEY,
                request("SHARED-DASHBOARD-ORDER", "Customer A")
        ).andExpect(status().isAccepted());
        performCreate(
                SECOND_TEST_API_KEY,
                request("SHARED-DASHBOARD-ORDER", "Customer B")
        ).andExpect(status().isAccepted());

        MockHttpSession companyASession = authenticatedDashboardSession(
                TEST_API_KEY
        );
        MockHttpSession companyBSession = authenticatedDashboardSession(
                SECOND_TEST_API_KEY
        );

        mockMvc.perform(get(
                        "/api/dashboard/orders/{externalOrderId}",
                        "SHARED-DASHBOARD-ORDER"
                ).session(companyASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.customerName")
                        .value("Customer A"));

        mockMvc.perform(get(
                        "/api/dashboard/orders/{externalOrderId}",
                        "SHARED-DASHBOARD-ORDER"
                ).session(companyBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.customerName")
                        .value("Customer B"));
    }

    @Test
    void dashboardCannotResendAnotherCompanyOrder() throws Exception {
        performCreate(
                SECOND_TEST_API_KEY,
                request("SECOND-COMPANY-ONLY", "Customer B")
        ).andExpect(status().isAccepted());
        MockHttpSession companyASession = authenticatedDashboardSession(
                TEST_API_KEY
        );
        CsrfData csrf = fetchCsrfToken(companyASession);

        mockMvc.perform(post(
                        "/api/dashboard/orders/{externalOrderId}/resend",
                        "SECOND-COMPANY-ONLY"
                )
                        .session(companyASession)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "communicationChannel": "EMAIL",
                              "responseDeadlineHours": 24
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ORDER_SNAPSHOT_NOT_FOUND"));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(confirmationRequestRepository.count()).isEqualTo(1);
    }

    @Test
    void emailSettingsAreIsolatedByAuthenticatedCompany()
            throws Exception {
        updateEmailSettings(TEST_API_KEY, "smtp.company-a.test")
                .andExpect(status().isNoContent());
        updateEmailSettings(
                SECOND_TEST_API_KEY,
                "smtp.company-b.test"
        ).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dispo/settings/email")
                        .header("X-API-Key", TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smtpHost")
                        .value("smtp.company-a.test"));
        mockMvc.perform(get("/api/dispo/settings/email")
                        .header("X-API-Key", SECOND_TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smtpHost")
                        .value("smtp.company-b.test"));

        assertThat(companyEmailSettingsRepository.findAll())
                .extracting(
                        settings -> settings.getCompany().getId(),
                        CompanyEmailSettings::getSmtpHost
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                TEST_COMPANY_ID,
                                "smtp.company-a.test"
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                secondCompany.getId(),
                                "smtp.company-b.test"
                        )
                );
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            String externalOrderId
    ) throws Exception {
        return performCreate(TEST_API_KEY, request(externalOrderId));
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            String apiKey,
            DispoConfirmationRequestDto request
    ) throws Exception {
        return mockMvc.perform(post("/api/dispo/confirmation-requests")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private DispoConfirmationRequestDto request(String externalOrderId) {
        return request(externalOrderId, "Security Test Customer");
    }

    private DispoConfirmationRequestDto request(
            String externalOrderId,
            String customerName
    ) {
        return new DispoConfirmationRequestDto(
                externalOrderId,
                "SECURITY-TOUR",
                "WUE-SE 1001",
                customerName,
                CommunicationChannel.EMAIL,
                "security-customer@example.com",
                null,
                "Security Street 1, 97070 Würzburg",
                "Heating oil",
                3_000,
                LocalDate.of(2099, Month.JUNE, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                24,
                "100 EUR"
        );
    }

    private MockHttpSession authenticatedDashboardSession(String apiKey)
            throws Exception {
        MvcResult accessResult = mockMvc.perform(
                        post("/api/dispo/dashboard-access")
                                .header("X-API-Key", apiKey)
                )
                .andExpect(status().isOk())
                .andReturn();
        String code = UriComponentsBuilder
                .fromUriString(accessResult.getResponse().getContentAsString())
                .build()
                .getQueryParams()
                .getFirst("code");
        assertThat(code).isNotBlank();

        MvcResult exchangeResult = mockMvc.perform(
                        post("/api/dashboard/auth/exchange")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(code)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(exchangeResult.getRequest().getSession(false))
                .isInstanceOf(MockHttpSession.class);
        return (MockHttpSession) exchangeResult.getRequest()
                .getSession(false);
    }

    private CsrfData fetchCsrfToken(MockHttpSession session)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/dashboard/csrf").session(session)
                )
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return new CsrfData(
                response.path("headerName").asText(),
                response.path("token").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    updateEmailSettings(String apiKey, String smtpHost) throws Exception {
        return mockMvc.perform(put("/api/dispo/settings/email")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "smtpHost": "%s",
                      "smtpPort": 25,
                      "securityMode": "NONE",
                      "authenticationEnabled": false,
                      "fromAddress": "sender@example.test",
                      "fromName": "Security Test Sender"
                    }
                    """.formatted(smtpHost)));
    }

    private record CsrfData(String headerName, String token) {
    }
}
