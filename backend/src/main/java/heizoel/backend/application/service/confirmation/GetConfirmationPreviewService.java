package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewResult;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewUseCase;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
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
        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        return new GetConfirmationPreviewResult(
                orderSnapshot.getExternalOrderId(),
                orderSnapshot.getCustomerName(),
                orderSnapshot.getDeliveryAddress(),
                orderSnapshot.getProduct(),
                orderSnapshot.getQuantityLiters(),
                confirmationRequest.getDeliveryDate(),
                confirmationRequest.getDeliveryWindowStart(),
                confirmationRequest.getDeliveryWindowEnd(),
                orderSnapshot.getPriceDisplayText(),
                orderSnapshot.getConfirmationStatus()
        );
    }
}
