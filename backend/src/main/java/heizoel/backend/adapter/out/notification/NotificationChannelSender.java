package heizoel.backend.adapter.out.notification;

import heizoel.backend.domain.*;

public interface NotificationChannelSender {

    CommunicationChannel channel();

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
