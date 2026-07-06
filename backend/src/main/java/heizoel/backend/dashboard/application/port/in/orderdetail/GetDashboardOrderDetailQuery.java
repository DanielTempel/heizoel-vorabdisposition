package heizoel.backend.dashboard.application.port.in.orderdetail;

import heizoel.backend.confirmation.application.model.CompanyContext;

public record GetDashboardOrderDetailQuery(
        CompanyContext companyContext,
        String externalOrderId
) {
}