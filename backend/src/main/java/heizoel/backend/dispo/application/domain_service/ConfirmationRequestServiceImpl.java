package heizoel.backend.dispo.application.domain_service;

import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.application.interfaces.TokenService;
import heizoel.backend.dispo.application.model.ConfirmationRequestData;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.repository.ConfirmationRequestRepository;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfirmationRequestServiceImpl implements ConfirmationRequestService {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final TokenService tokenService;
    private final ConfirmationProperties confirmationProperties;

    @Override
    public Optional<ConfirmationRequest> findActiveRequest(OrderSnapshot orderSnapshot) {
        return confirmationRequestRepository.findByOrderSnapshotAndActiveTrue(orderSnapshot);
    }

    @Override
    public ConfirmationRequest create(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData data
    ) {
        Instant sentAt = Instant.now();

        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.setOrderSnapshot(orderSnapshot);
        confirmationRequest.setToken(tokenService.generateToken());
        confirmationRequest.setDeliveryDate(data.deliveryDate());
        confirmationRequest.setDeliveryWindowStart(data.deliveryWindowStart());
        confirmationRequest.setDeliveryWindowEnd(data.deliveryWindowEnd());
        confirmationRequest.setActive(true);
        confirmationRequest.setSentAt(sentAt);
        confirmationRequest.setExpiresAt(sentAt.plus(confirmationProperties.getResponseDeadline()));

        return confirmationRequestRepository.save(confirmationRequest);
    }

    @Override
    public void markInactive(ConfirmationRequest confirmationRequest) {
        confirmationRequest.setActive(false);
        confirmationRequestRepository.save(confirmationRequest);
    }

    @Override
    public boolean hasSameData(
            ConfirmationRequest confirmationRequest,
            ConfirmationRequestData data
    ) {
        return confirmationRequest.getDeliveryDate().equals(data.deliveryDate())
                && confirmationRequest.getDeliveryWindowStart().equals(data.deliveryWindowStart())
                && confirmationRequest.getDeliveryWindowEnd().equals(data.deliveryWindowEnd());
    }

    @Override
    public Optional<ConfirmationRequest> findByToken(String token) {
        return confirmationRequestRepository.findByToken(token);
    }

    @Override
    public Optional<ConfirmationRequest> findById(Long id) {
        return confirmationRequestRepository.findById(id);
    }
}
