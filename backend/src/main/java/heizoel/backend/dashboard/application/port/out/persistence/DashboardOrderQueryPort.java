package heizoel.backend.dashboard.application.port.out.persistence;

import heizoel.backend.dashboard.application.port.in.DashboardOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface DashboardOrderQueryPort {

    Page<DashboardOrder> findDashboardOrders(
            DashboardOrderFilter filter,
            Pageable pageable
    );


}
