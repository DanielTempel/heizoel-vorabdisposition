package heizoel.backend.application.port.out.notification;

import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;

public interface NotificationService {

    void sendConfirmationRequest(
            Order order,
            ConfirmationRequest confirmationRequest
    );

    void sendCustomerResponseReceived(
            Order order,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    );

}

