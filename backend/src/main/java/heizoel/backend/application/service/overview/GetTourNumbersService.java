package heizoel.backend.application.service.overview;


import heizoel.backend.application.exception.InvalidFilterException;
import heizoel.backend.application.port.in.overview.GetTourNumbersQuery;
import heizoel.backend.application.port.in.overview.GetTourNumbersUseCase;
import heizoel.backend.application.port.out.persistence.TourNumberFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTourNumbersService implements GetTourNumbersUseCase {

    private final TourOverviewQueryPort tourOverviewQueryPort;
    private final Clock clock;

    @Override
    public List<String> getTourNumbers(GetTourNumbersQuery query) {

        LocalDate dateFrom = query.dateFrom() != null
                ? query.dateFrom()
                : LocalDate.now(clock);

        LocalDate dateTo = query.dateTo();

        validateDateRange(
                query.companyContext().companyId(),
                dateFrom,
                dateTo
        );

        String search = normalizeSearch(query.search());

        return tourOverviewQueryPort.findTourNumbers(
                new TourNumberFilter(
                        query.companyContext().companyId(),
                        search,
                        dateFrom,
                        dateTo
                )
        );
    }

    private void validateDateRange(
            Long companyId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn(
                    "Rejecting tour number query: reason=INVALID_DATE_RANGE, companyId={}, dateFrom={}, dateTo={}",
                    companyId,
                    dateFrom,
                    dateTo
            );
            throw new InvalidFilterException(
                    "Date from must not be after date to."
            );
        }
    }

    private String normalizeSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }

}
