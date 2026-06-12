package heizoel.backend.notification.application;

import heizoel.backend.customer.domain.CustomerResponseType;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.notification.application.interfaces.NotificationService;
import heizoel.backend.notification.application.interfaces.EmailSender;
import heizoel.backend.notification.application.interfaces.SmsConfirmationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailSender emailSender;
    private final SmsConfirmationSender smsSender;

    @Override
    public void sendConfirmationRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        switch (confirmationRequest.getCommunicationChannel()) {
            case EMAIL -> emailSender.sendConfirmationRequest(orderSnapshot, confirmationRequest);
            case SMS -> smsSender.send(orderSnapshot, confirmationRequest);
        }
    }

    @Override
    public void sendCustomerResponseReceived(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType
    ) {

        switch (confirmationRequest.getCommunicationChannel()) {
            case EMAIL -> emailSender.sendCustomerResponseReceived(
                    orderSnapshot,
                    confirmationRequest,
                    responseType
            );
            case SMS -> log.info(
                    "Customer response follow-up SMS skipped because it is not implemented in the MVP. externalOrderId={}, responseType={}",
                    orderSnapshot.getExternalOrderId(),
                    responseType
            );
        }
    }

}