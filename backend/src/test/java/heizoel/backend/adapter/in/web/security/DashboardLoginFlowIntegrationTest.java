package heizoel.backend.adapter.in.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "camunda.bpm.job-execution.enabled=false")
@AutoConfigureMockMvc
@Import(DashboardLoginFlowIntegrationTest.MutableClockConfiguration.class)
@Sql(
        scripts = "/db/test/configure-security-test-company.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class DashboardLoginFlowIntegrationTest {

    private static final Instant INITIAL_TIME =
            Instant.parse("2026-08-17T10:00:00Z");
    private static final LocalDate DELIVERY_DATE =
            LocalDate.of(2026, 8, 17);
    private static final String COMPANY_A_API_KEY =
            "security-tenant-api-key";
    private static final String COMPANY_B_API_KEY =
            "other-security-tenant-api-key";
    private static final long COMPANY_A_ID = 1001L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("heizoel_backend_test")
                    .withUsername("heizoel")
                    .withPassword("heizoel");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    ApiKeyHasher apiKeyHasher;

    @Autowired
    MutableClock clock;

    @MockitoBean
    ResendConfirmationRequestUseCase resendConfirmationRequestUseCase;

    @BeforeEach
    void setUp() {
        clock.set(INITIAL_TIME);
        confirmationRequestRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void dashboardAccess_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/dispo/dashboard-access"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
    }

    @Test
    void dashboardAccess_withInvalidApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/dispo/dashboard-access")
                        .header("X-API-Key", "invalid-api-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    @Test
    void dashboardAccess_withValidApiKey_returnsRedirectUrl()
            throws Exception {
        MvcResult result = performDashboardAccess(COMPANY_A_API_KEY)
                .andExpect(status().isOk())
                .andReturn();

        UriComponents redirect = UriComponentsBuilder
                .fromUriString(result.getResponse().getContentAsString())
                .build();

        assertThat(redirect.getScheme()).isEqualTo("http");
        assertThat(redirect.getHost()).isEqualTo("localhost");
        assertThat(redirect.getPort()).isEqualTo(3000);
        assertThat(redirect.getPath()).isEqualTo("/login");
        assertThat(redirect.getQueryParams().getFirst("code"))
                .matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void dashboardExchange_withValidCode_createsAuthenticatedSession()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );

        assertThat(session.getId()).isNotBlank();

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();
        assertThat(securityContext.getAuthentication().getPrincipal())
                .isEqualTo(new CompanyContext(COMPANY_A_ID));
    }

    @Test
    void dashboardEndpoint_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardEndpoint_withAuthenticatedSession_isAccessible()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );

        MvcResult result = mockMvc.perform(get("/api/dashboard/tours")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isSameAs(session);
    }

    @Test
    void dashboardAccessCode_canOnlyBeUsedOnce() throws Exception {
        String code = createAccessCode(COMPANY_A_API_KEY);

        exchange(code).andExpect(status().isNoContent());
        exchange(code).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredDashboardAccessCode_isRejected() throws Exception {
        String code = createAccessCode(COMPANY_A_API_KEY);

        clock.set(INITIAL_TIME.plusSeconds(120));

        exchange(code).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardSession_usesCorrectCompanyContext() throws Exception {
        Company companyA = companyRepository.findById(COMPANY_A_ID)
                .orElseThrow();
        Company companyB = companyRepository.save(Company.create(
                "Dashboard Security Company B",
                apiKeyHasher.hash(COMPANY_B_API_KEY),
                "http://localhost:8082/api/dispo/callback"
        ));

        createOrder(companyA, "ORDER-A", "TOUR-A", "TOKEN-A");
        createOrder(companyB, "ORDER-B", "TOUR-B", "TOKEN-B");

        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );

        mockMvc.perform(get("/api/dashboard/tours")
                        .param("dateFrom", DELIVERY_DATE.toString())
                        .param("dateTo", DELIVERY_DATE.toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].tourNumber")
                        .value(contains("TOUR-A")))
                .andExpect(jsonPath("$.items[*].orders[*].externalOrderId")
                        .value(contains("ORDER-A")));
    }

    @Test
    void dashboardCsrf_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/csrf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardCsrf_withAuthenticatedSession_returnsToken()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );

        CsrfData csrf = fetchCsrfToken(session);

        assertThat(csrf.headerName()).isEqualTo("X-CSRF-TOKEN");
        assertThat(csrf.parameterName()).isEqualTo("_csrf");
        assertThat(csrf.token()).isNotBlank();
    }

    @Test
    void dashboardResend_withSessionWithoutCsrf_returns403()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );
        fetchCsrfToken(session);

        mockMvc.perform(resendRequest(session))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resendConfirmationRequestUseCase);
    }

    @Test
    void dashboardResend_withSessionAndInvalidCsrf_returns403()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );
        CsrfData csrf = fetchCsrfToken(session);

        mockMvc.perform(resendRequest(session)
                        .header(csrf.headerName(), "invalid-csrf-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resendConfirmationRequestUseCase);
    }

    @Test
    void dashboardResend_withSessionAndValidCsrf_isAccepted()
            throws Exception {
        MockHttpSession session = exchangeForSession(
                createAccessCode(COMPANY_A_API_KEY)
        );
        CsrfData csrf = fetchCsrfToken(session);
        when(resendConfirmationRequestUseCase.resend(any()))
                .thenReturn(new ResendConfirmationRequestResult(
                        "ORDER-CSRF",
                        ConfirmationStatus.OPEN
                ));

        mockMvc.perform(resendRequest(session)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.externalOrderId")
                        .value("ORDER-CSRF"))
                .andExpect(jsonPath("$.confirmationStatus")
                        .value("OPEN"));

        verify(resendConfirmationRequestUseCase).resend(
                new ResendConfirmationRequestCommand(
                        new CompanyContext(COMPANY_A_ID),
                        "ORDER-CSRF",
                        CommunicationChannel.SMS,
                        24
                )
        );
    }

    private ResultActions performDashboardAccess(String apiKey)
            throws Exception {
        return mockMvc.perform(post("/api/dispo/dashboard-access")
                .header("X-API-Key", apiKey));
    }

    private String createAccessCode(String apiKey) throws Exception {
        MvcResult result = performDashboardAccess(apiKey)
                .andExpect(status().isOk())
                .andReturn();

        String code = UriComponentsBuilder
                .fromUriString(result.getResponse().getContentAsString())
                .build()
                .getQueryParams()
                .getFirst("code");

        assertThat(code).isNotBlank();
        return code;
    }

    private ResultActions exchange(String code) throws Exception {
        return mockMvc.perform(post("/api/dashboard/auth/exchange")
                .contentType(MediaType.TEXT_PLAIN)
                .content(code));
    }

    private MockHttpSession exchangeForSession(String code) throws Exception {
        MvcResult result = exchange(code)
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getRequest().getSession(false))
                .isInstanceOf(MockHttpSession.class);

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private CsrfData fetchCsrfToken(MockHttpSession session)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/csrf")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isSameAs(session);

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return new CsrfData(
                response.path("headerName").asText(),
                response.path("parameterName").asText(),
                response.path("token").asText()
        );
    }

    private MockHttpServletRequestBuilder resendRequest(
            MockHttpSession session
    ) {
        return post(
                "/api/dashboard/orders/{externalOrderId}/resend",
                "ORDER-CSRF"
        )
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "communicationChannel": "SMS",
                          "responseDeadlineHours": 24
                        }
                        """);
    }

    private void createOrder(
            Company company,
            String externalOrderId,
            String tourNumber,
            String token
    ) {
        Order order = orderRepository.save(Order.create(
                company,
                externalOrderId,
                Tour.of(tourNumber, "WUE-LOGIN 1"),
                "Dashboard Customer " + externalOrderId,
                "dashboard@example.test",
                "+49123456789",
                "Dashboard Street 1, 97070 Würzburg",
                "Heating oil",
                2_500,
                "2,500 EUR"
        ));

        confirmationRequestRepository.save(
                ConfirmationRequest.createPending(
                        order,
                        token,
                        CommunicationChannel.EMAIL,
                        DeliverySlot.of(
                                DELIVERY_DATE,
                                LocalTime.of(8, 0),
                                LocalTime.of(9, 0)
                        ),
                        24
                )
        );
    }

    private record CsrfData(
            String headerName,
            String parameterName,
            String token
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(INITIAL_TIME, ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        private MutableClock(Instant initialInstant, ZoneId zone) {
            this.instant = new AtomicReference<>(initialInstant);
            this.zone = zone;
        }

        void set(Instant newInstant) {
            instant.set(newInstant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant.get(), newZone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
