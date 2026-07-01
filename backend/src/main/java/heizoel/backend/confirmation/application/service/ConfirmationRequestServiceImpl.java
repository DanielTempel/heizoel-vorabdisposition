package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.out.TokenService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfirmationRequestServiceImpl implements ConfirmationRequestService {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final TokenService tokenService;

    @Override
    public Optional<ConfirmationRequest> findLatestRequest(OrderSnapshot orderSnapshot) {
        return confirmationRequestRepository.findTopByOrderSnapshotOrderByIdDesc(orderSnapshot);
    }

    @Override
    public ConfirmationRequest create(
            OrderSnapshot orderSnapshot,
            ConfirmationRequestData data
    ) {
        Instant sentAt = Instant.now();
        Instant deliveryStartsAt = data.deliveryDate()
                .atTime(data.deliveryWindowStart())
                .atZone(DELIVERY_ZONE)
                .toInstant();

        if (!deliveryStartsAt.isAfter(sentAt)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }

        Instant requestedExpiresAt =
                sentAt.plus(Duration.ofHours(data.responseDeadline()));
        Instant effectiveExpiresAt = requestedExpiresAt.isBefore(deliveryStartsAt)
                ? requestedExpiresAt
                : deliveryStartsAt;

        ConfirmationRequest confirmationRequest = new ConfirmationRequest();
        confirmationRequest.setOrderSnapshot(orderSnapshot);
        confirmationRequest.setToken(tokenService.generateToken());
        confirmationRequest.setDeliveryDate(data.deliveryDate());
        confirmationRequest.setDeliveryWindowStart(data.deliveryWindowStart());
        confirmationRequest.setDeliveryWindowEnd(data.deliveryWindowEnd());
        confirmationRequest.setCommunicationChannel(data.communicationChannel());
        confirmationRequest.setActive(true);
        confirmationRequest.setSentAt(sentAt);
        confirmationRequest.setExpiresAt(effectiveExpiresAt);
        confirmationRequest.setResponseDeadlineHours((data.responseDeadline()));

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
                && confirmationRequest.getDeliveryWindowEnd().equals(data.deliveryWindowEnd())
                && confirmationRequest.getCommunicationChannel() == data.communicationChannel()
                && confirmationRequest.getResponseDeadlineHours().equals(data.responseDeadline());
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

