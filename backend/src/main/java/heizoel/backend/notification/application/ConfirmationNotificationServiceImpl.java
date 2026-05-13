package heizoel.backend.notification.application;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import heizoel.backend.notification.application.interfaces.EmailConfirmationSender;
import heizoel.backend.notification.application.interfaces.SmsConfirmationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmationNotificationServiceImpl implements ConfirmationNotificationService {

    private final EmailConfirmationSender emailSender;
    private final SmsConfirmationSender smsSender;

    @Override
    public void sendConfirmationRequest(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        switch (confirmationRequest.getCommunicationChannel()) {
            case EMAIL -> emailSender.send(orderSnapshot, confirmationRequest);
            case SMS -> smsSender.send(orderSnapshot, confirmationRequest);
        }
    }
}