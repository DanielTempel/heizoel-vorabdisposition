package heizoel.backend.notification.application.interfaces;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;

public interface EmailConfirmationSender {

    void send(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    );
}
