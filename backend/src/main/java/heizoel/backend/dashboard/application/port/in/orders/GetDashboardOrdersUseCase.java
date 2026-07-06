package heizoel.backend.dashboard.application.port.in.orders;

public interface GetDashboardOrdersUseCase {

    DashboardOrdersPageResult getDashboardOrders(GetDashboardOrdersQuery query);
}