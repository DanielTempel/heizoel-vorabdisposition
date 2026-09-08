package heizoel.backend.adapter.in.web.dispo;

import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;
import heizoel.backend.adapter.in.web.security.DashboardAccessService;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestUseCase;
import heizoel.backend.domain.ConfirmationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispo")
@RequiredArgsConstructor
@Slf4j
public class DispoController {

    private final CreateConfirmationRequestUseCase createConfirmationRequestUseCase;
    private final DashboardAccessService dashboardAccessService;

    @PostMapping("/confirmation-requests")
    public ResponseEntity<DispoConfirmationResponseDto> createConfirmationRequest(
            @AuthenticationPrincipal CompanyContext companyContext,
            @Valid @RequestBody DispoConfirmationRequestDto request
    ) {
        log.info(
                "Started createConfirmationRequest: companyId={}, externalOrderId={}, tourNumber={}, communicationChannel={}, deliveryDate={}, responseDeadlineHours={}",
                companyContext.companyId(),
                request.externalOrderId(),
                request.tourNumber(),
                request.communicationChannel(),
                request.deliveryDate(),
                request.responseDeadlineHours()
        );
        CreateConfirmationRequestCommand command = new CreateConfirmationRequestCommand(
                companyContext,
                request.externalOrderId(),
                request.tourNumber(),
                request.vehicleLicensePlate(),
                request.customerName(),
                request.communicationChannel(),
                request.customerEmail(),
                request.customerPhoneNumber(),
                request.deliveryAddress(),
                request.product(),
                request.quantityLiters(),
                request.deliveryDate(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.responseDeadlineHours(),
                request.priceDisplayText()
        );

        CreateConfirmationRequestResult result =
                createConfirmationRequestUseCase.createConfirmationRequest(command);

        HttpStatus status =
                result.confirmationStatus() == ConfirmationStatus.OPEN
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.OK;

        DispoConfirmationResponseDto response = new DispoConfirmationResponseDto(
                result.externalOrderId(),
                result.confirmationStatus()
        );

        log.info(
                "Completed createConfirmationRequest: companyId={}, externalOrderId={}, confirmationStatus={}, status={}",
                companyContext.companyId(),
                result.externalOrderId(),
                result.confirmationStatus(),
                status.value()
        );
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/dashboard-access")
    public String createDashboardAccess(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        log.info(
                "Started createDashboardAccess: companyId={}",
                companyContext.companyId()
        );
        String redirectUrl = dashboardAccessService.createRedirectUrl(
                companyContext.companyId()
        );

        log.info(
                "Completed createDashboardAccess: companyId={}, status=200",
                companyContext.companyId()
        );
        return redirectUrl;
    }


}
