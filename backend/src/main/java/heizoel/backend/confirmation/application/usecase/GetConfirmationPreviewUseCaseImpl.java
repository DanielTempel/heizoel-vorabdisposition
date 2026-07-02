package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.customer.GetConfirmationPreviewResult;
import heizoel.backend.confirmation.application.port.in.customer.GetConfirmationPreviewUseCase;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetConfirmationPreviewUseCaseImpl implements GetConfirmationPreviewUseCase {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;

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
