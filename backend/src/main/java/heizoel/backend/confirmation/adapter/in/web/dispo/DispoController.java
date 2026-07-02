package heizoel.backend.confirmation.adapter.in.web.dispo;

import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.confirmation.application.port.in.confirmation.DispoConfirmationRequestUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispo/confirmation-requests")
@RequiredArgsConstructor
public class DispoController {

    private final DispoConfirmationRequestUseCase dispoConfirmationRequestUseCase;

    @PostMapping
    public ResponseEntity<DispoConfirmationResponseDto> createConfirmationRequest(
            @Valid @RequestBody DispoConfirmationRequestDto request
    ) {
        CreateConfirmationRequestCommand command = new CreateConfirmationRequestCommand(
                request.externalOrderId(),
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
                dispoConfirmationRequestUseCase.createConfirmationRequest(command);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        DispoConfirmationResponseDto response = new DispoConfirmationResponseDto(
                result.externalOrderId(),
                result.confirmationStatus()
        );

        return ResponseEntity.status(status).body(response);
    }
}
