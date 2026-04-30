package heizoel.backend.dispo.api.dto;

import heizoel.backend.dispo.api.dto.request.DispoConfirmationRequestDto;
import heizoel.backend.dispo.api.dto.response.DispoConfirmationResponseDto;
import heizoel.backend.dispo.domain.ConfirmationStatus;
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

    @PostMapping
    public ResponseEntity<DispoConfirmationResponseDto> createConfirmationRequest(
            @Valid @RequestBody DispoConfirmationRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DispoConfirmationResponseDto(
                        request.externalOrderId(),
                        ConfirmationStatus.SENT
                ));
    }
}