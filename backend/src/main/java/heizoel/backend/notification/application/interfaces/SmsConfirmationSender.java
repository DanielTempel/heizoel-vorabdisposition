package heizoel.backend.notification.application.interfaces;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;

public interface SmsConfirmationSender {

    void send(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    );

}
