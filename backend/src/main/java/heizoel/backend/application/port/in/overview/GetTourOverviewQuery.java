package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.ConfirmationStatus;

import java.time.LocalDate;
import java.util.Set;

public record GetTourOverviewQuery(
        CompanyContext companyContext,
        Set<String> tourNumbers,
        Set<ConfirmationStatus> statuses,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page
) {
}