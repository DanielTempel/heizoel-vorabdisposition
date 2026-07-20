package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.model.overview.ConfirmationOverviewPage;

public interface GetConfirmationOverviewUseCase {

    ConfirmationOverviewPage getDashboardOrders(GetDashboardOrdersQuery query);
}