package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewResult;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewUseCase;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.*;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetConfirmationPreviewService implements GetConfirmationPreviewUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final CustomerResponseRepository customerResponseRepository;

    @Override
    @Transactional(readOnly = true)
    public GetConfirmationPreviewResult getConfirmationPreview(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findLatestByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
        Order order = confirmationRequest.getOrder();
        DeliverySlot deliverySlot = confirmationRequest.getDeliverySlot();

        CustomerResponse customerResponse =
                customerResponseRepository
                        .findByConfirmationRequest(confirmationRequest)
                        .orElse(null);

        ConfirmationStatus confirmationStatus =
                ConfirmationStatus.fromRequest(
                        confirmationRequest.isActive(),
                        customerResponse != null
                                ? customerResponse.getResponseType()
                                : null
                );

        return new GetConfirmationPreviewResult(
                order.getExternalOrderId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getProduct(),
                order.getQuantityLiters(),
                deliverySlot.getDate(),
                deliverySlot.getStart(),
                deliverySlot.getEnd(),
                order.getPriceDisplayText(),
                confirmationStatus
        );
    }
}
