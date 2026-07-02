package heizoel.backend.confirmation.adapter.in.web.customer;

import heizoel.backend.confirmation.application.port.out.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmationRequestResolver {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;

    public ConfirmationRequest resolveByToken(String token) {
        return confirmationRequestRepository.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
    }
}

