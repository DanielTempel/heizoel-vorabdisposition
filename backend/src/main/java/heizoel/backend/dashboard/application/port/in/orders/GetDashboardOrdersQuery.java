package heizoel.backend.dashboard.application.port.in.orders;

import heizoel.backend.confirmation.application.model.CompanyContext;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

import java.time.LocalDate;

public record GetDashboardOrdersQuery(
        CompanyContext companyContext,
        ConfirmationStatus status,
        String search,
        LocalDate deliveryDate,
        int page
) {
}