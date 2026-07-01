package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;

public interface SmsConfirmationSender {

    void send(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    );

}

