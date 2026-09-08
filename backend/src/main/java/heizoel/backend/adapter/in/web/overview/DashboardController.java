package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.overview.dto.ResendConfirmationRequestRequestDto;
import heizoel.backend.adapter.in.web.overview.dto.detail.DashboardOrderDetailResponseDto;
import heizoel.backend.adapter.in.web.overview.dto.detail.ResendConfirmationRequestResponseDto;
import heizoel.backend.adapter.in.web.security.DashboardAccessService;
import heizoel.backend.adapter.in.web.security.DashboardAuthenticationService;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.adapter.in.web.overview.dto.ToursPageResponseDto;
import heizoel.backend.application.model.overview.TourOverviewPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final GetTourOverviewUseCase getTourOverviewUseCase;
    private final GetTourNumbersUseCase getTourNumbersUseCase;
    private final GetConfirmationDetailUseCase getConfirmationDetailUseCase;
    private final ResendConfirmationRequestUseCase resendConfirmationRequestUseCase;
    private final DashboardAccessService dashboardAccessService;
    private final DashboardAuthenticationService dashboardAuthenticationService;

    @GetMapping("/tours")
    public ToursPageResponseDto getTours(
            @AuthenticationPrincipal CompanyContext companyContext,
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
        log.info(
                "Started getTours: companyId={}, tourNumbers={}, statuses={}, searchPresent={}, dateFrom={}, dateTo={}, page={}",
                companyContext.companyId(),
                tourNumbers,
                statuses,
                search != null,
                dateFrom,
                dateTo,
                page
        );
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
        ToursPageResponseDto response = ToursPageResponseDto.from(result);

        log.info(
                "Completed getTours: companyId={}, itemCount={}, page={}, size={}, totalElements={}, totalPages={}, status=200",
                companyContext.companyId(),
                response.items().size(),
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
        return response;
    }

    @GetMapping("/tour-numbers")
    public List<String> getTourNumbers(
            @AuthenticationPrincipal CompanyContext companyContext,
            @RequestParam(required = false) String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo
    ) {
        log.info(
                "Started getTourNumbers: companyId={}, searchPresent={}, dateFrom={}, dateTo={}",
                companyContext.companyId(),
                search != null,
                dateFrom,
                dateTo
        );
        List<String> result = getTourNumbersUseCase.getTourNumbers(
                new GetTourNumbersQuery(
                        companyContext,
                        search,
                        dateFrom,
                        dateTo
                )
        );

        log.info(
                "Completed getTourNumbers: companyId={}, itemCount={}, status=200",
                companyContext.companyId(),
                result.size()
        );
        return result;
    }

    @GetMapping("/orders/{externalOrderId}")
    public DashboardOrderDetailResponseDto getOrderDetail(
            @AuthenticationPrincipal CompanyContext companyContext,
            @PathVariable String externalOrderId
    ) {
        log.info(
                "Started getOrderDetail: companyId={}, externalOrderId={}",
                companyContext.companyId(),
                externalOrderId
        );
        ConfirmationDetail detail = getConfirmationDetailUseCase.getOrderDetail(
                        new GetConfirmationDetailQuery(
                                companyContext,
                                externalOrderId
                        )
                );
        DashboardOrderDetailResponseDto response = DashboardOrderDetailResponseDto.from(detail);

        log.info(
                "Completed getOrderDetail: companyId={}, externalOrderId={}, confirmationStatus={}, currentRequestPresent={}, previousRequestCount={}, status=200",
                companyContext.companyId(),
                detail.order().externalOrderId(),
                detail.order().confirmationStatus(),
                detail.currentRequest() != null,
                detail.previousRequests().size()
        );
        return response;
    }

    @PostMapping("/orders/{externalOrderId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResendConfirmationRequestResponseDto resendConfirmationRequest(
            @AuthenticationPrincipal CompanyContext companyContext,
            @PathVariable String externalOrderId,
            @Valid @RequestBody ResendConfirmationRequestRequestDto request
    ) {
        log.info(
                "Started resendConfirmationRequest: companyId={}, externalOrderId={}, communicationChannel={}, responseDeadlineHours={}",
                companyContext.companyId(),
                externalOrderId,
                request.communicationChannel(),
                request.responseDeadlineHours()
        );
        ResendConfirmationRequestResult result =
                resendConfirmationRequestUseCase.resend(
                        new ResendConfirmationRequestCommand(
                                companyContext,
                                externalOrderId,
                                request.communicationChannel(),
                                request.responseDeadlineHours()
                        )
                );

        ResendConfirmationRequestResponseDto response = new ResendConfirmationRequestResponseDto(
                result.externalOrderId(),
                result.confirmationStatus()
        );

        log.info(
                "Completed resendConfirmationRequest: companyId={}, externalOrderId={}, confirmationStatus={}, status=202",
                companyContext.companyId(),
                result.externalOrderId(),
                result.confirmationStatus()
        );
        return response;
    }

    @PostMapping(
            value = "/auth/exchange",
            consumes = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exchangeDashboardAccess(
            @RequestBody String code,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info("Started exchangeDashboardAccess");
        CompanyContext companyContext = dashboardAccessService.consume(code);

        dashboardAuthenticationService.authenticate(
                companyContext,
                request,
                response
        );
        log.info(
                "Completed exchangeDashboardAccess: companyId={}, status=204",
                companyContext.companyId()
        );
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        log.info("Started csrf");
        log.info("Completed csrf: status=200");
        return csrfToken;
    }

}
