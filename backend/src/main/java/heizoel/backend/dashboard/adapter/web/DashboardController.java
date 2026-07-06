package heizoel.backend.dashboard.adapter.web;


import heizoel.backend.confirmation.adapter.web.security.CompanyContextResolver;
import heizoel.backend.confirmation.application.model.CompanyContext;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.dashboard.adapter.web.dto.DashboardOrderDetailResponseDto;
import heizoel.backend.dashboard.adapter.web.dto.DashboardOrdersPageResponseDto;
import heizoel.backend.dashboard.application.port.in.orderdetail.DashboardOrderDetail;
import heizoel.backend.dashboard.application.port.in.orderdetail.GetDashboardOrderDetailQuery;
import heizoel.backend.dashboard.application.port.in.orderdetail.GetDashboardOrderDetailUseCase;
import heizoel.backend.dashboard.application.port.in.orders.DashboardOrdersPageResult;
import heizoel.backend.dashboard.application.port.in.orders.GetDashboardOrdersQuery;
import heizoel.backend.dashboard.application.port.in.orders.GetDashboardOrdersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dispo/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CompanyContextResolver companyContextResolver;
    private final GetDashboardOrdersUseCase getDashboardOrdersUseCase;
    private final GetDashboardOrderDetailUseCase getDashboardOrderDetailUseCase;

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

        DashboardOrdersPageResult result = getDashboardOrdersUseCase.getDashboardOrders(
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

        DashboardOrderDetail detail = getDashboardOrderDetailUseCase.getOrderDetail(
                new GetDashboardOrderDetailQuery(
                        companyContext,
                        externalOrderId
                )
        );

        return DashboardOrderDetailResponseDto.from(detail);
    }

}