package heizoel.backend.application.service.overview;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.InvalidFilterException;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewPage;
import heizoel.backend.application.port.in.overview.GetTourOverviewQuery;
import heizoel.backend.application.port.out.persistence.TourOverviewFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewQueryPort;
import heizoel.backend.domain.ConfirmationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTourOverviewServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T10:00:00Z"),
            BUSINESS_ZONE
    );

    @Mock
    TourOverviewQueryPort tourOverviewQueryPort;

    GetTourOverviewService service;

    @BeforeEach
    void setUp() {
        service = new GetTourOverviewService(tourOverviewQueryPort, CLOCK);
    }

    @Test
    void usesClockDateWhenDateFromIsMissing() {
        PortCall call = call(query(null, null, null, null, 0));

        assertThat(call.filter().dateFrom()).isEqualTo(TODAY);
    }

    @Test
    void keepsExplicitDateFrom() {
        LocalDate dateFrom = LocalDate.of(2026, 7, 1);

        PortCall call = call(query(null, null, dateFrom, null, 0));

        assertThat(call.filter().dateFrom()).isEqualTo(dateFrom);
    }

    @Test
    void rejectsInvertedDateRangeWithoutCallingPort() {
        GetTourOverviewQuery query = query(
                null,
                null,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 4),
                0
        );

        assertThatThrownBy(() -> service.getTours(query))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Date from must not be after date to.");
        verify(tourOverviewQueryPort, never()).findTours(any(), any());
    }

    @Test
    void normalizesBlankSearchToNull() {
        PortCall call = call(query(null, "   ", TODAY, null, 0));

        assertThat(call.filter().search()).isNull();
    }

    @Test
    void trimsSearch() {
        PortCall call = call(query(null, "  Müller  ", TODAY, null, 0));

        assertThat(call.filter().search()).isEqualTo("Müller");
    }

    @Test
    void normalizesNullStatusesToEmptySet() {
        PortCall call = call(query(null, null, TODAY, null, 0));

        assertThat(call.filter().statuses()).isEmpty();
    }

    @Test
    void passesAllRequestedStatuses() {
        Set<ConfirmationStatus> statuses = Set.of(
                ConfirmationStatus.REJECTED,
                ConfirmationStatus.NO_RESPONSE
        );

        PortCall call = call(query(statuses, null, TODAY, null, 0));

        assertThat(call.filter().statuses()).containsExactlyInAnyOrderElementsOf(statuses);
    }

    @Test
    void clampsNegativePageToZero() {
        PortCall call = call(query(null, null, TODAY, null, -5));

        assertThat(call.pageable().getPageNumber()).isZero();
        assertThat(call.pageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void passesRequestedPageWithFixedPageSize() {
        PortCall call = call(query(null, null, TODAY, null, 2));

        assertThat(call.pageable().getPageNumber()).isEqualTo(2);
        assertThat(call.pageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void mapsPortPageToOverviewPage() {
        TourOverviewItem tour = new TourOverviewItem(
                "A-17",
                "WUE-AB 123",
                TODAY,
                List.of()
        );
        Page<TourOverviewItem> portResult = new PageImpl<>(
                List.of(tour),
                PageRequest.of(2, 20),
                41
        );
        when(tourOverviewQueryPort.findTours(any(), any())).thenReturn(portResult);

        TourOverviewPage result = service.getTours(
                query(null, null, TODAY, null, 2)
        );

        assertThat(result.items()).containsExactly(tour);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    private PortCall call(GetTourOverviewQuery query) {
        when(tourOverviewQueryPort.findTours(any(), any())).thenReturn(Page.empty());

        service.getTours(query);

        ArgumentCaptor<TourOverviewFilter> filterCaptor =
                ArgumentCaptor.forClass(TourOverviewFilter.class);
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(tourOverviewQueryPort).findTours(
                filterCaptor.capture(),
                pageableCaptor.capture()
        );
        return new PortCall(filterCaptor.getValue(), pageableCaptor.getValue());
    }

    private GetTourOverviewQuery query(
            Set<ConfirmationStatus> statuses,
            String search,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page
    ) {
        return new GetTourOverviewQuery(
                new CompanyContext(7L),
                statuses,
                search,
                dateFrom,
                dateTo,
                page
        );
    }

    private record PortCall(
            TourOverviewFilter filter,
            Pageable pageable
    ) {
    }
}
