package heizoel.backend.dashboard.application.usecase;


import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.dashboard.application.port.in.DashboardOrder;
import heizoel.backend.dashboard.application.port.in.DashboardOrdersPageResult;
import heizoel.backend.dashboard.application.port.in.GetDashboardOrdersQuery;
import heizoel.backend.dashboard.application.port.in.GetDashboardOrdersUseCase;
import heizoel.backend.dashboard.application.port.out.persistence.DashboardOrderFilter;
import heizoel.backend.dashboard.application.port.out.persistence.DashboardOrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class GetDashboardOrdersUseCaseImpl implements GetDashboardOrdersUseCase {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Berlin");
    private static final int PAGE_SIZE = 20;

    private final DashboardOrderQueryPort dashboardOrderQueryPort;

    @Override
    public DashboardOrdersPageResult getDashboardOrders(GetDashboardOrdersQuery query) {
        int page = Math.max(query.page(), 0);
        String search = normalizeSearch(query.search());

        DashboardOrderFilter filter = new DashboardOrderFilter(
                query.companyContext().companyId(),
                LocalDate.now(BUSINESS_ZONE),
                query.deliveryDate(),
                query.status(),
                search
        );

        Page<DashboardOrder> result = dashboardOrderQueryPort.findDashboardOrders(
                filter,
                PageRequest.of(page, PAGE_SIZE)
        );

        return new DashboardOrdersPageResult(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }
}