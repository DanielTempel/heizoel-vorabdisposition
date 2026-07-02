package heizoel.backend.confirmation.adapter.notification;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;

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
