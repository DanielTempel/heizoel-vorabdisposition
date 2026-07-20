package heizoel.backend.adapter.out.notification;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.OrderSnapshot;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.CustomerResponseType;

public interface NotificationChannelSender {

    CommunicationChannel channel();

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
