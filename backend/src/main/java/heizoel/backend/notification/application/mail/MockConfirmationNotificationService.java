package heizoel.backend.notification.application.mail;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.notification.application.interfaces.ConfirmationNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("smtp")
@Service
public class MockConfirmationNotificationService implements ConfirmationNotificationService {

    @Override
    public void sendConfirmationRequestEmail(
            OrderSnapshot orderSnapshot,
            ConfirmationRequest confirmationRequest
    ) {
        String confirmationUrl = "/confirmation/" + confirmationRequest.getToken();

        log.info(
                "Mock e-mail sent to {} for order {}. Confirm URL: {}",
                orderSnapshot.getCustomerEmail(),
                orderSnapshot.getExternalOrderId(),
                confirmationUrl
        );
    }
}