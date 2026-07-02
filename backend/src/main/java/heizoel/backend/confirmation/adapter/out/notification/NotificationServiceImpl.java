package heizoel.backend.confirmation.adapter.out.notification;

import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.application.port.out.NotificationService;
import heizoel.backend.confirmation.application.port.out.EmailSender;
import heizoel.backend.confirmation.application.port.out.SmsConfirmationSender;
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
