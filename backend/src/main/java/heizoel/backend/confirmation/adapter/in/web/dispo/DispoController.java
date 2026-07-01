package heizoel.backend.confirmation.adapter.in.web.dispo;

import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationResponseDto;
import heizoel.backend.confirmation.application.port.in.DispoConfirmationService;
import heizoel.backend.confirmation.application.model.DispoConfirmationCreationResult;
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

    private final DispoConfirmationService confirmationService;

    @PostMapping
    public ResponseEntity<DispoConfirmationResponseDto> createConfirmationRequest(
            @Valid @RequestBody DispoConfirmationRequestDto request
    ) {

        DispoConfirmationCreationResult result  = confirmationService.createConfirmationRequest(request);

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(result.response());
    }
}
