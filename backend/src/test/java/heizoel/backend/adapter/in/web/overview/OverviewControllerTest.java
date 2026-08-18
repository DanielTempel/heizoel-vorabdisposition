package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationToken;
import heizoel.backend.adapter.in.web.security.DashboardAccessService;
import heizoel.backend.adapter.in.web.security.DashboardAuthenticationService;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.InvalidFilterException;
import heizoel.backend.application.model.overview.OrderOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewPage;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.application.port.in.overview.GetConfirmationDetailUseCase;
import heizoel.backend.application.port.in.overview.GetTourNumbersQuery;
import heizoel.backend.application.port.in.overview.GetTourNumbersUseCase;
import heizoel.backend.application.port.in.overview.GetTourOverviewQuery;
import heizoel.backend.application.port.in.overview.GetTourOverviewUseCase;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class OverviewControllerTest {

    private static final CompanyContext COMPANY_CONTEXT = new CompanyContext(7L);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetTourOverviewUseCase getTourOverviewUseCase;

    @MockitoBean
    GetTourNumbersUseCase getTourNumbersUseCase;

    @MockitoBean
    GetConfirmationDetailUseCase getConfirmationDetailUseCase;

    @MockitoBean
    ResendConfirmationRequestUseCase resendConfirmationRequestUseCase;

    @MockitoBean
    DashboardAccessService dashboardAccessService;

    @MockitoBean
    DashboardAuthenticationService dashboardAuthenticationService;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                ApiKeyAuthenticationToken.authenticated(COMPANY_CONTEXT)
        );
        when(getTourOverviewUseCase.getTours(any())).thenReturn(
                new TourOverviewPage(List.of(), 0, 20, 0, 0)
        );
        when(getTourNumbersUseCase.getTourNumbers(any())).thenReturn(List.of());
        when(clock.instant()).thenReturn(Instant.parse("2026-08-04T10:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getsToursWithDefaultParametersAndPageMetadata() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        GetTourOverviewQuery query = capturedQuery();
        assertThat(query.companyContext()).isEqualTo(COMPANY_CONTEXT);
        assertThat(query.tourNumbers()).isNull();
        assertThat(query.statuses()).isNull();
        assertThat(query.search()).isNull();
        assertThat(query.dateFrom()).isNull();
        assertThat(query.dateTo()).isNull();
        assertThat(query.page()).isZero();
    }

    @Test
    void returnsOpenOrderWithoutIncreasingBusinessStatusCounts() throws Exception {
        OrderOverviewItem openOrder = new OrderOverviewItem(
                "ORDER-OPEN",
                "Customer",
                "Delivery address",
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                CommunicationChannel.EMAIL,
                ConfirmationStatus.OPEN,
                null
        );
        TourOverviewItem tour = new TourOverviewItem(
                "TOUR-OPEN",
                "WÜ-DEMO 100",
                LocalDate.of(2026, 8, 5),
                List.of(openOrder)
        );
        when(getTourOverviewUseCase.getTours(any())).thenReturn(
                new TourOverviewPage(List.of(tour), 0, 20, 1, 1)
        );

        mockMvc.perform(get("/api/dashboard/tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orders[0].confirmationStatus")
                        .value("OPEN"))
                .andExpect(jsonPath("$.items[0].statusCounts.sent").value(0))
                .andExpect(jsonPath("$.items[0].statusCounts.confirmed").value(0))
                .andExpect(jsonPath("$.items[0].statusCounts.rejected").value(0))
                .andExpect(jsonPath("$.items[0].statusCounts.noResponse").value(0));
    }

    @Test
    void bindsSeveralStatuses() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("statuses", "REJECTED", "NO_RESPONSE"))
                .andExpect(status().isOk());

        assertThat(capturedQuery().statuses()).containsExactlyInAnyOrder(
                ConfirmationStatus.REJECTED,
                ConfirmationStatus.NO_RESPONSE
        );
    }

    @Test
    void bindsSeveralTourNumbers() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("tourNumbers", "A-17", "NORD-3"))
                .andExpect(status().isOk());

        assertThat(capturedQuery().tourNumbers())
                .containsExactlyInAnyOrder("A-17", "NORD-3");
    }

    @Test
    void bindsIsoDates() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("dateFrom", "2026-08-01")
                        .param("dateTo", "2026-08-31"))
                .andExpect(status().isOk());

        GetTourOverviewQuery query = capturedQuery();
        assertThat(query.dateFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(query.dateTo()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void passesSearchWithoutNormalizingIt() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("search", "Müller"))
                .andExpect(status().isOk());

        assertThat(capturedQuery().search()).isEqualTo("Müller");
    }

    @Test
    void rejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("statuses", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(getTourOverviewUseCase, never()).getTours(any());
    }

    @Test
    void rejectsNonIsoDate() throws Exception {
        mockMvc.perform(get("/api/dashboard/tours")
                        .param("dateFrom", "04.08.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(getTourOverviewUseCase, never()).getTours(any());
    }

    @Test
    void returnsValidationErrorForInvalidDateRange() throws Exception {
        when(getTourOverviewUseCase.getTours(any())).thenThrow(
                new InvalidFilterException("Date from must not be after date to.")
        );

        mockMvc.perform(get("/api/dashboard/tours")
                        .param("dateFrom", "2026-08-05")
                        .param("dateTo", "2026-08-04"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Date from must not be after date to."))
                .andExpect(jsonPath("$.timestamp")
                        .value("2026-08-04T10:00:00Z"));
    }

    @Test
    void bindsTourNumberOptionsParameters() throws Exception {
        mockMvc.perform(get("/api/dashboard/tour-numbers")
                        .param("search", " A-1 ")
                        .param("dateFrom", "2026-08-01")
                        .param("dateTo", "2026-08-31"))
                .andExpect(status().isOk());

        GetTourNumbersQuery query = capturedTourNumbersQuery();
        assertThat(query.companyContext()).isEqualTo(COMPANY_CONTEXT);
        assertThat(query.search()).isEqualTo(" A-1 ");
        assertThat(query.dateFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(query.dateTo()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void returnsTourNumbersAsJsonArray() throws Exception {
        when(getTourNumbersUseCase.getTourNumbers(any())).thenReturn(
                List.of("A-17", "NORD-3")
        );

        mockMvc.perform(get("/api/dashboard/tour-numbers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("A-17"))
                .andExpect(jsonPath("$[1]").value("NORD-3"));
    }

    @Test
    void rejectsNonIsoDateForTourNumbers() throws Exception {
        mockMvc.perform(get("/api/dashboard/tour-numbers")
                        .param("dateFrom", "05.08.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(getTourNumbersUseCase, never()).getTourNumbers(any());
    }

    private GetTourOverviewQuery capturedQuery() {
        ArgumentCaptor<GetTourOverviewQuery> captor =
                ArgumentCaptor.forClass(GetTourOverviewQuery.class);
        verify(getTourOverviewUseCase).getTours(captor.capture());
        return captor.getValue();
    }

    private GetTourNumbersQuery capturedTourNumbersQuery() {
        ArgumentCaptor<GetTourNumbersQuery> captor =
                ArgumentCaptor.forClass(GetTourNumbersQuery.class);
        verify(getTourNumbersUseCase).getTourNumbers(captor.capture());
        return captor.getValue();
    }
}
