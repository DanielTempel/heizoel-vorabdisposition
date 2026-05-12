package heizoel.backend.notification.application.interfaces;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;

public interface ConfirmationNotificationService {

    void sendConfirmationRequestEmail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    );

}
