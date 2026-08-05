package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.overview.dto.detail.DashboardOrderDetailResponseDto;
import heizoel.backend.adapter.in.web.security.CompanyContextResolver;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.adapter.in.web.overview.dto.ToursPageResponseDto;
import heizoel.backend.application.model.overview.TourOverviewPage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dispo/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CompanyContextResolver companyContextResolver;
    private final GetTourOverviewUseCase getTourOverviewUseCase;
    private final GetTourNumbersUseCase getTourNumbersUseCase;
    private final GetConfirmationDetailUseCase getConfirmationDetailUseCase;

    @GetMapping("/tours")
    public ToursPageResponseDto getTours(
            @RequestParam(required = false) Set<String> tourNumbers,
            @RequestParam(required = false) Set<ConfirmationStatus> statuses,
            @RequestParam(required = false) String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(defaultValue = "0") int page
    ) {
        CompanyContext companyContext = companyContextResolver.resolve();

        TourOverviewPage result = getTourOverviewUseCase.getTours(
                new GetTourOverviewQuery(
                        companyContext,
                        tourNumbers,
                        statuses,
                        search,
                        dateFrom,
                        dateTo,
                        page
                )
        );

        return ToursPageResponseDto.from(result);
    }

    @GetMapping("/tour-numbers")
    public List<String> getTourNumbers(
            @RequestParam(required = false) String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo
    ) {
        CompanyContext companyContext = companyContextResolver.resolve();

        return getTourNumbersUseCase.getTourNumbers(
                new GetTourNumbersQuery(
                        companyContext,
                        search,
                        dateFrom,
                        dateTo
                )
        );
    }

    @GetMapping("/orders/{externalOrderId}")
    public DashboardOrderDetailResponseDto getOrderDetail(
            @PathVariable String externalOrderId
    ) {
        CompanyContext companyContext = companyContextResolver.resolve();

        ConfirmationDetail detail = getConfirmationDetailUseCase.getOrderDetail(
                        new GetConfirmationDetailQuery(
                                companyContext,
                                externalOrderId
                        )
                );

        return DashboardOrderDetailResponseDto.from(detail);
    }

}
