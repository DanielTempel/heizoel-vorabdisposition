package heizoel.backend.confirmation.adapter.in.web.customer;

import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
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

