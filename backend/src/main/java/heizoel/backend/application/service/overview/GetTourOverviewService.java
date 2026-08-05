package heizoel.backend.application.service.overview;


import heizoel.backend.application.exception.InvalidFilterException;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewPage;
import heizoel.backend.application.port.in.overview.GetTourOverviewQuery;
import heizoel.backend.application.port.in.overview.GetTourOverviewUseCase;
import heizoel.backend.application.port.out.persistence.TourOverviewFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewQueryPort;
import heizoel.backend.domain.ConfirmationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetTourOverviewService implements GetTourOverviewUseCase {

    private static final int PAGE_SIZE = 20;

    private final TourOverviewQueryPort tourOverviewQueryPort;
    private final Clock clock;

    @Override
    public TourOverviewPage getTours(GetTourOverviewQuery query) {
        int page = Math.max(query.page(), 0);

        LocalDate dateFrom = query.dateFrom() != null
                ? query.dateFrom()
                : LocalDate.now(clock);

        LocalDate dateTo = query.dateTo();
        validateDateRange(dateFrom, dateTo);

        Set<ConfirmationStatus> statuses =
                query.statuses() == null
                        ? Set.of()
                        : Set.copyOf(query.statuses());

        String search = normalizeSearch(query.search());

        Set<String> tourNumbers = query.tourNumbers() == null
                ? Set.of()
                : query.tourNumbers()
                .stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        TourOverviewFilter filter = new TourOverviewFilter(
                query.companyContext().companyId(),
                tourNumbers,
                statuses,
                search,
                dateFrom,
                dateTo
        );

        Page<TourOverviewItem> result = tourOverviewQueryPort.findTours(
                        filter,
                        PageRequest.of(page, PAGE_SIZE)
                );

        return new TourOverviewPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validateDateRange(
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateTo != null && dateFrom.isAfter(dateTo)) {
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
