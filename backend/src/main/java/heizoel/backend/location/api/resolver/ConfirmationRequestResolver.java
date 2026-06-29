package heizoel.backend.location.api.resolver;

import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.exceptions.customer.ConfirmationRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmationRequestResolver {

    private final ConfirmationRequestService confirmationRequestService;

    public ConfirmationRequest resolveByToken(String token) {
        return confirmationRequestService.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
    }
}
