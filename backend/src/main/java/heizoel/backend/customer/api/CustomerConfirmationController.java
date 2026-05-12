package heizoel.backend.customer.api;

import heizoel.backend.customer.api.dto.CustomerAnswerRequestDto;
import heizoel.backend.customer.application.interfaces.CustomerConfirmationService;
import heizoel.backend.customer.api.dto.CustomerConfirmationPreviewDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
public class CustomerConfirmationController {

    private final CustomerConfirmationService customerConfirmationService;

    @GetMapping("/{token}")
    public CustomerConfirmationPreviewDto getConfirmationPreview(
            @PathVariable String token

    ) {
        return customerConfirmationService.getConfirmationPreview(token);
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<Void> confirm(
            @PathVariable String token,
            @Valid @RequestBody(required = false) CustomerAnswerRequestDto request
    ) {
        String customerComment = request != null ? request.customerComment() : null;
        customerConfirmationService.confirm(token, customerComment);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable String token,
            @Valid @RequestBody(required = false) CustomerAnswerRequestDto request
    ) {
        String customerComment = request != null ? request.customerComment() : null;
        customerConfirmationService.reject(token, customerComment);
        return ResponseEntity.noContent().build();
    }
}
