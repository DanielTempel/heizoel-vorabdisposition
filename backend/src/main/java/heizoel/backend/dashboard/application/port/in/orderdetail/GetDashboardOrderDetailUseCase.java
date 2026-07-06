package heizoel.backend.dashboard.application.port.in.orderdetail;

public interface GetDashboardOrderDetailUseCase {

    DashboardOrderDetail getOrderDetail(GetDashboardOrderDetailQuery query);

}
