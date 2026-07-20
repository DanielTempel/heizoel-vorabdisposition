package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.context.CompanyContext;

public record GetDashboardOrderDetailQuery(
        CompanyContext companyContext,
        String externalOrderId
) {
}