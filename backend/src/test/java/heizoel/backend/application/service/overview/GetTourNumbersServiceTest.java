package heizoel.backend.application.service.overview;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.InvalidFilterException;
import heizoel.backend.application.port.in.overview.GetTourNumbersQuery;
import heizoel.backend.application.port.out.persistence.TourNumberFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTourNumbersServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-05T10:00:00Z"),
            ZoneId.of("Europe/Berlin")
    );

    @Mock
    TourOverviewQueryPort tourOverviewQueryPort;

    GetTourNumbersService service;

    @BeforeEach
    void setUp() {
        service = new GetTourNumbersService(tourOverviewQueryPort, CLOCK);
    }

    @Test
    void usesClockDateWhenDateFromIsMissing() {
        TourNumberFilter filter = call(query(null, null, null));

        assertThat(filter.companyId()).isEqualTo(7L);
        assertThat(filter.dateFrom()).isEqualTo(TODAY);
        assertThat(filter.dateTo()).isNull();
    }

    @Test
    void trimsSearch() {
        TourNumberFilter filter = call(query("  A-1  ", TODAY, null));

        assertThat(filter.search()).isEqualTo("A-1");
    }

    @Test
    void normalizesBlankSearchToNull() {
        TourNumberFilter filter = call(query("   ", TODAY, null));

        assertThat(filter.search()).isNull();
    }

    @Test
    void rejectsInvertedDateRangeWithoutCallingPort() {
        GetTourNumbersQuery query = query(
                null,
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 5)
        );

        assertThatThrownBy(() -> service.getTourNumbers(query))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessage("Date from must not be after date to.");
        verify(tourOverviewQueryPort, never()).findTourNumbers(any());
    }

    private TourNumberFilter call(GetTourNumbersQuery query) {
        when(tourOverviewQueryPort.findTourNumbers(any())).thenReturn(List.of());

        service.getTourNumbers(query);

        ArgumentCaptor<TourNumberFilter> captor =
                ArgumentCaptor.forClass(TourNumberFilter.class);
        verify(tourOverviewQueryPort).findTourNumbers(captor.capture());
        return captor.getValue();
    }

    private GetTourNumbersQuery query(
            String search,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return new GetTourNumbersQuery(
                new CompanyContext(7L),
                search,
                dateFrom,
                dateTo
        );
    }
}
