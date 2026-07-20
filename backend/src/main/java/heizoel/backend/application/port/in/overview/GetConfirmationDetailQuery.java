package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.context.CompanyContext;

public record GetConfirmationDetailQuery(
        CompanyContext companyContext,
        String externalOrderId
) {
}