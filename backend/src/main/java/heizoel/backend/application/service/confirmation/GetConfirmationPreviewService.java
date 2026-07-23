package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewResult;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewUseCase;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetConfirmationPreviewService implements GetConfirmationPreviewUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public GetConfirmationPreviewResult getConfirmationPreview(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
        Order order = confirmationRequest.getOrder();

        return new GetConfirmationPreviewResult(
                order.getExternalOrderId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getProduct(),
                order.getQuantityLiters(),
                confirmationRequest.getDeliveryDate(),
                confirmationRequest.getDeliveryWindowStart(),
                confirmationRequest.getDeliveryWindowEnd(),
                order.getPriceDisplayText(),
                order.getConfirmationStatus()
        );
    }
}
