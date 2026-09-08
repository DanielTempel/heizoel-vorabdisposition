package heizoel.backend.adapter.in.web.customer;

import heizoel.backend.adapter.in.web.customer.dto.CustomerResponseRequestDto;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseUseCase;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewResult;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewUseCase;
import heizoel.backend.adapter.in.web.customer.dto.CustomerConfirmationPreviewDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/confirmations")
@RequiredArgsConstructor
@Slf4j
public class CustomerConfirmationController {

    private final SubmitCustomerResponseUseCase submitCustomerResponseUseCase;
    private final GetConfirmationPreviewUseCase getConfirmationPreviewUseCase;

    @GetMapping("/{token}")
    public CustomerConfirmationPreviewDto getConfirmationPreview(
            @PathVariable String token
    ) {
        log.info("Started getConfirmationPreview");
        GetConfirmationPreviewResult result =
                getConfirmationPreviewUseCase.getConfirmationPreview(token);

        CustomerConfirmationPreviewDto response = new CustomerConfirmationPreviewDto(
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

        log.info(
                "Completed getConfirmationPreview: externalOrderId={}, confirmationStatus={}, status=200",
                result.externalOrderId(),
                result.confirmationStatus()
        );
        return response;
    }

    @PostMapping("/{token}/response")
    public ResponseEntity<Void> submitResponse(
            @PathVariable String token,
            @Valid @RequestBody CustomerResponseRequestDto request
    ) {
        log.info(
                "Started submitResponse: responseType={}",
                request.responseType()
        );
        submitCustomerResponseUseCase.submitCustomerResponse(
                new SubmitCustomerResponseCommand(
                        token,
                        request.responseType(),
                        request.customerComment()
                )
        );

        log.info("Completed submitResponse: status=204");
        return ResponseEntity.noContent().build();
    }
}

