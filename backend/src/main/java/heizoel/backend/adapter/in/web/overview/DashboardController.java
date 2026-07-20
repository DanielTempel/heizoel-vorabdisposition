package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.security.CompanyContextResolver;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.adapter.in.web.overview.dto.DashboardOrderDetailResponseDto;
import heizoel.backend.adapter.in.web.overview.dto.DashboardOrdersPageResponseDto;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.overview.GetDashboardOrderDetailQuery;
import heizoel.backend.application.port.in.overview.GetConfirmationDetailUseCase;
import heizoel.backend.application.model.overview.ConfirmationOverviewPage;
import heizoel.backend.application.port.in.overview.GetDashboardOrdersQuery;
import heizoel.backend.application.port.in.overview.GetConfirmationOverviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dispo/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CompanyContextResolver companyContextResolver;
    private final GetConfirmationOverviewUseCase getConfirmationOverviewUseCase;
    private final GetConfirmationDetailUseCase getConfirmationDetailUseCase;

    @GetMapping("/orders")
    public DashboardOrdersPageResponseDto getOrders(
            @RequestParam(required = false) ConfirmationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate deliveryDate,
            @RequestParam(defaultValue = "0") int page
    ) {
        CompanyContext companyContext = companyContextResolver.resolve();

        ConfirmationOverviewPage result = getConfirmationOverviewUseCase.getDashboardOrders(
                new GetDashboardOrdersQuery(
                        companyContext,
                        status,
                        search,
                        deliveryDate,
                        page
                )
        );

        return DashboardOrdersPageResponseDto.from(result);
    }


    @GetMapping("/orders/{externalOrderId}")
    public DashboardOrderDetailResponseDto getOrderDetail(
            @PathVariable String externalOrderId
    ) {
        CompanyContext companyContext = companyContextResolver.resolve();

        ConfirmationDetail detail = getConfirmationDetailUseCase.getOrderDetail(
                new GetDashboardOrderDetailQuery(
                        companyContext,
                        externalOrderId
                )
        );

        return DashboardOrderDetailResponseDto.from(detail);
    }

}