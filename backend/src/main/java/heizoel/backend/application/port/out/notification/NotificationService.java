package heizoel.backend.application.port.out.notification;

import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;

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

