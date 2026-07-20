package heizoel.backend.application.service.overview;


import heizoel.backend.application.model.overview.ConfirmationOverviewItem;
import heizoel.backend.application.model.overview.ConfirmationOverviewPage;
import heizoel.backend.application.port.in.overview.GetDashboardOrdersQuery;
import heizoel.backend.application.port.in.overview.GetConfirmationOverviewUseCase;
import heizoel.backend.application.port.out.persistence.ConfirmationOverviewFilter;
import heizoel.backend.application.port.out.persistence.ConfirmationOverviewQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class GetConfirmationOverviewService implements GetConfirmationOverviewUseCase {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Berlin");
    private static final int PAGE_SIZE = 20;

    private final ConfirmationOverviewQueryPort confirmationOverviewQueryPort;

    @Override
    public ConfirmationOverviewPage getDashboardOrders(GetDashboardOrdersQuery query) {
        int page = Math.max(query.page(), 0);
        String search = normalizeSearch(query.search());

        ConfirmationOverviewFilter filter = new ConfirmationOverviewFilter(
                query.companyContext().companyId(),
                LocalDate.now(BUSINESS_ZONE),
                query.deliveryDate(),
                query.status(),
                search
        );

        Page<ConfirmationOverviewItem> result = confirmationOverviewQueryPort.findOverview(
                filter,
                PageRequest.of(page, PAGE_SIZE)
        );

        return new ConfirmationOverviewPage(
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