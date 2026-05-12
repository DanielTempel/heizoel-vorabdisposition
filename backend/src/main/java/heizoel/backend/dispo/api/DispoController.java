package heizoel.backend.dispo.api;

import heizoel.backend.dispo.api.dto.request.DispoConfirmationRequestDto;
import heizoel.backend.dispo.api.dto.response.DispoConfirmationResponseDto;
import heizoel.backend.dispo.application.interfaces.DispoConfirmationService;
import heizoel.backend.dispo.application.model.command.DispoConfirmationCreationResult;
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