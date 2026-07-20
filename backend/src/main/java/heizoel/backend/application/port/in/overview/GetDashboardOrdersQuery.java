package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.ConfirmationStatus;

import java.time.LocalDate;

public record GetDashboardOrdersQuery(
        CompanyContext companyContext,
        ConfirmationStatus status,
        String search,
        LocalDate deliveryDate,
        int page
) {
}