package heizoel.backend.confirmation.adapter.in.web.customer;

import heizoel.backend.confirmation.adapter.in.web.customer.dto.CustomerResponseRequestDto;
import heizoel.backend.confirmation.application.port.in.customer.SubmitCustomerResponseCommand;
import heizoel.backend.confirmation.application.port.in.customer.SubmitCustomerResponseUseCase;
import heizoel.backend.confirmation.application.port.in.customer.GetConfirmationPreviewResult;
import heizoel.backend.confirmation.application.port.in.customer.GetConfirmationPreviewUseCase;
import heizoel.backend.confirmation.adapter.in.web.customer.dto.CustomerConfirmationPreviewDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
public class CustomerConfirmationController {

    private final SubmitCustomerResponseUseCase submitCustomerResponseUseCase;
    private final GetConfirmationPreviewUseCase getConfirmationPreviewUseCase;

    @GetMapping("/{token}")
    public CustomerConfirmationPreviewDto getConfirmationPreview(
            @PathVariable String token
    ) {
        GetConfirmationPreviewResult result =
                getConfirmationPreviewUseCase.getConfirmationPreview(token);

        return new CustomerConfirmationPreviewDto(
                result.externalOrderId(),
                result.customerName(),
                result.deliveryAddress(),
                result.product(),
                result.quantityLiters(),
                result.deliveryDate(),
                result.deliveryWindowStart(),
                result.deliveryWindowEnd(),
                result.priceDisplayText(),
                result.confirmationStatus()
        );
    }

    @PostMapping("/{token}/response")
    public ResponseEntity<Void> submitResponse(
            @PathVariable String token,
            @Valid @RequestBody CustomerResponseRequestDto request
    ) {
        submitCustomerResponseUseCase.submitCustomerResponse(
                new SubmitCustomerResponseCommand(
                        token,
                        request.responseType(),
                        request.customerComment()
                )
        );

        return ResponseEntity.noContent().build();
    }
}

