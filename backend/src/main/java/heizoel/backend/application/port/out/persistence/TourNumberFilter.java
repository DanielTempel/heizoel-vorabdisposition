package heizoel.backend.application.port.out.persistence;

import java.time.LocalDate;

public record TourNumberFilter(
        Long companyId,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}