package heizoel.backend.confirmation.application.port.out.notification;

import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;

public interface NotificationService {

    void sendConfirmationRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    );

    void sendCustomerResponseReceived(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    );

}

