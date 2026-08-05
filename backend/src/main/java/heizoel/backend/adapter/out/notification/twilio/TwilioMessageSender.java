package heizoel.backend.adapter.out.notification.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.base.Creator;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.configuration.properties.TwilioProperties;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TwilioMessageSender {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final TwilioRestClient twilioRestClient;
    private final ConfirmationProperties confirmationProperties;
    private final TwilioProperties twilioProperties;
    private final ObjectMapper objectMapper;

    public void sendSms(
            Order order,
            ConfirmationRequest confirmationRequest,
            String messageBody
    ) {
        sendMessage(
                order,
                confirmationRequest,
                CommunicationChannel.SMS,
                twilioProperties.getSmsFrom(),
                order.getCustomerPhoneNumber(),
                messageBody
        );
    }

    public void sendWhatsApp(
            Order order,
            ConfirmationRequest confirmationRequest,
            String messageBody
    ) {
        sendMessage(
                order,
                confirmationRequest,
                CommunicationChannel.WHATSAPP,
                twilioProperties.getWhatsappFrom(),
                whatsappAddress(order.getCustomerPhoneNumber()),
                messageBody
        );
    }

    private void sendMessage(
            Order order,
            ConfirmationRequest confirmationRequest,
            CommunicationChannel channel,
            String from,
            String to,
            String messageBody
    ) {
        validateConfiguration(channel, from, to);

        try {
            log.info(
                    "Sending Twilio notification. externalOrderId={}, channel={}, from={}, to={}, contentTemplateSidPresent={}",
                    order.getExternalOrderId(),
                    channel,
                    maskAddress(from),
                    maskAddress(to),
                    hasText(twilioProperties.getContentTemplateSid())
            );
            createMessage(order, confirmationRequest, from, to, messageBody)
                    .create(twilioRestClient);
        } catch (ApiException ex) {
            log.error(
                    "Twilio delivery failed. externalOrderId={}, channel={}, from={}, to={}, contentTemplateSid={}, twilioCode={}, twilioStatus={}, possibleCause={}",
                    order.getExternalOrderId(),
                    channel,
                    maskAddress(from),
                    maskAddress(to),
                    twilioProperties.getContentTemplateSid(),
                    ex.getCode(),
                    ex.getStatusCode(),
                    determinePossibleCause(channel, from, to),
                    ex
            );
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio notification could not be delivered for externalOrderId="
                            + order.getExternalOrderId(),
                    ex
            );
        }
    }

    private Creator<Message> createMessage(
            Order order,
            ConfirmationRequest confirmationRequest,
            String from,
            String to,
            String messageBody
    ) {
        var messageCreator = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(from),
                hasText(twilioProperties.getContentTemplateSid()) ? null : messageBody
        );

        if (hasText(twilioProperties.getContentTemplateSid())) {
            messageCreator.setContentSid(twilioProperties.getContentTemplateSid());
            messageCreator.setContentVariables(
                    buildContentVariables(order, confirmationRequest)
            );
        }

        return messageCreator;
    }

    private void validateConfiguration(
            CommunicationChannel channel,
            String from,
            String to
    ) {
        if (!hasText(twilioProperties.getAccountSid())
                || !hasText(twilioProperties.getAuthToken())
                || !hasText(from)
                || !hasText(to)) {
            log.error(
                    "Twilio configuration is incomplete. channel={}, hasAccountSid={}, hasAuthToken={}, hasFrom={}, hasTo={}, from={}, to={}",
                    channel,
                    hasText(twilioProperties.getAccountSid()),
                    hasText(twilioProperties.getAuthToken()),
                    hasText(from),
                    hasText(to),
                    maskAddress(from),
                    maskAddress(to)
            );
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio configuration is incomplete for channel " + channel,
                    new IllegalStateException("Missing required Twilio properties.")
            );
        }
    }

    private String buildContentVariables(
            Order order,
            ConfirmationRequest confirmationRequest
    ) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", order.getCustomerName());
        variables.put("2", order.getExternalOrderId());
        variables.put("3", order.getProduct());
        variables.put("4", String.valueOf(order.getQuantityLiters()));
        variables.put(
                "5",
                confirmationRequest.getDeliverySlot().getDate().format(DATE_FORMAT)
        );
        variables.put(
                "6",
                confirmationRequest.getDeliverySlot().getStart().format(TIME_FORMAT)
                        + " - "
                        + confirmationRequest.getDeliverySlot().getEnd().format(TIME_FORMAT)
        );
        variables.put("7", order.getDeliveryAddress());
        variables.put(
                "8",
                buildConfirmationLink(confirmationRequest)
        );

        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Twilio content variables could not be serialized.", ex);
        }
    }

    private String buildConfirmationLink(ConfirmationRequest confirmationRequest) {
        return confirmationProperties.getFrontendUrl()
                + "/confirmation/"
                + confirmationRequest.getToken();
    }

    private String whatsappAddress(String phoneNumber) {
        return hasText(phoneNumber) && phoneNumber.startsWith("whatsapp:")
                ? phoneNumber
                : "whatsapp:" + phoneNumber;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String determinePossibleCause(
            CommunicationChannel channel,
            String from,
            String to
    ) {
        if (!hasText(from)) {
            return "sender address is missing";
        }
        if (!hasText(to)) {
            return "recipient address is missing";
        }
        if (channel == CommunicationChannel.WHATSAPP) {
            if (!to.startsWith("whatsapp:+")) {
                return "recipient WhatsApp number is not in expected E.164 format";
            }
            if (!from.startsWith("whatsapp:+")) {
                return "configured WhatsApp sender is not in expected Twilio format";
            }
            if (hasText(twilioProperties.getContentTemplateSid())) {
                return "WhatsApp template may be unapproved, not enabled for the sender, or the recipient may not be joined to the sandbox";
            }
            return "recipient may not be joined to the Twilio WhatsApp sandbox or the sender may not be enabled for production WhatsApp";
        }
        if (channel == CommunicationChannel.SMS && !to.startsWith("+")) {
            return "recipient SMS number is not in expected E.164 format";
        }
        return "check Twilio account credentials, sender provisioning, and destination reachability";
    }

    private String maskAddress(String value) {
        if (!hasText(value)) {
            return "<empty>";
        }
        int visibleDigits = 4;
        int prefixLength = value.startsWith("whatsapp:") ? "whatsapp:".length() : 0;
        String prefix = value.substring(0, prefixLength);
        String rest = value.substring(prefixLength);

        if (rest.length() <= visibleDigits) {
            return prefix + "****";
        }

        return prefix + "*".repeat(Math.max(0, rest.length() - visibleDigits))
                + rest.substring(rest.length() - visibleDigits);
    }
}
