package heizoel.backend.dashboard.application.port.out.persistence;

import heizoel.backend.dashboard.application.port.in.orders.DashboardOrderRaw;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface DashboardOrderQueryPort {

    Page<DashboardOrderRaw> findDashboardOrders(
            DashboardOrderFilter filter,
            Pageable pageable
    );


}
