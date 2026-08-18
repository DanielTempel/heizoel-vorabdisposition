package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.ConfirmationStatus;

import java.time.LocalDate;
import java.util.Set;


public record TourOverviewFilter(
        Long companyId,
        Set<String> tourNumbers,
        Set<ConfirmationStatus> statuses,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}