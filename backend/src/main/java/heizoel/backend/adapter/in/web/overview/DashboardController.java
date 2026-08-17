package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.overview.dto.ResendConfirmationRequestRequestDto;
import heizoel.backend.adapter.in.web.overview.dto.detail.DashboardOrderDetailResponseDto;
import heizoel.backend.adapter.in.web.overview.dto.detail.ResendConfirmationRequestResponseDto;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.adapter.in.web.overview.dto.ToursPageResponseDto;
import heizoel.backend.application.model.overview.TourOverviewPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dispo/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GetTourOverviewUseCase getTourOverviewUseCase;
    private final GetTourNumbersUseCase getTourNumbersUseCase;
    private final GetConfirmationDetailUseCase getConfirmationDetailUseCase;
    private final ResendConfirmationRequestUseCase resendConfirmationRequestUseCase;

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
            @AuthenticationPrincipal CompanyContext companyContext,
            @RequestParam(required = false) String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo
    ) {
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
            @AuthenticationPrincipal CompanyContext companyContext,
            @PathVariable String externalOrderId
    ) {
        ConfirmationDetail detail = getConfirmationDetailUseCase.getOrderDetail(
                        new GetConfirmationDetailQuery(
                                companyContext,
                                externalOrderId
                        )
                );

        return DashboardOrderDetailResponseDto.from(detail);
    }

    @PostMapping("/orders/{externalOrderId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResendConfirmationRequestResponseDto resendConfirmationRequest(
            @AuthenticationPrincipal CompanyContext companyContext,
            @PathVariable String externalOrderId,
            @Valid @RequestBody ResendConfirmationRequestRequestDto request
    ) {
        ResendConfirmationRequestResult result =
                resendConfirmationRequestUseCase.resend(
                        new ResendConfirmationRequestCommand(
                                companyContext,
                                externalOrderId,
                                request.communicationChannel(),
                                request.responseDeadlineHours()
                        )
                );

        return new ResendConfirmationRequestResponseDto(
                result.externalOrderId(),
                result.confirmationStatus()
        );
    }

}
