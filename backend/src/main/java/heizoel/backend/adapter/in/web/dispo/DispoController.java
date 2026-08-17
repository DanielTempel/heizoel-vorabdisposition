package heizoel.backend.adapter.in.web.dispo;

import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestUseCase;
import heizoel.backend.domain.ConfirmationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispo/confirmation-requests")
@RequiredArgsConstructor
public class DispoController {

    private final CreateConfirmationRequestUseCase createConfirmationRequestUseCase;

    @PostMapping
    public ResponseEntity<DispoConfirmationResponseDto> createConfirmationRequest(
            @AuthenticationPrincipal CompanyContext companyContext,
            @Valid @RequestBody DispoConfirmationRequestDto request
    ) {
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

        return ResponseEntity.status(status).body(response);
    }
}
