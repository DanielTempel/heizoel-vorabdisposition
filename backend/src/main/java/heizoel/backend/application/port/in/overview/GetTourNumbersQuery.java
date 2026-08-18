package heizoel.backend.application.port.in.overview;


import heizoel.backend.application.context.CompanyContext;

import java.time.LocalDate;

public record GetTourNumbersQuery(
        CompanyContext companyContext,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}