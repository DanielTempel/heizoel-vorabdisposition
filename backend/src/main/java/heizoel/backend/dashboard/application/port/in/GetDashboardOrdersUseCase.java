package heizoel.backend.dashboard.application.port.in;

public interface GetDashboardOrdersUseCase {

    DashboardOrdersPageResult getDashboardOrders(GetDashboardOrdersQuery query);
}