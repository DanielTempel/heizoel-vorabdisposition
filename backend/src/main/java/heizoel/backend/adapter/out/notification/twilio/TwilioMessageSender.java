package heizoel.backend.adapter.out.notification.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.base.Creator;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import heizoel.backend.adapter.out.notification.ConfirmationMessageContent;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.configuration.properties.TwilioProperties;
import heizoel.backend.domain.CommunicationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class TwilioMessageSender {

    private static final String WHATSAPP_PREFIX = "whatsapp:";
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private final TwilioRestClient twilioRestClient;
    private final TwilioProperties twilioProperties;
    private final ObjectMapper objectMapper;

    public void sendSms(
            String externalOrderId,
            String to,
            ConfirmationMessageContent content
    ) {
        sendMessage(
                externalOrderId,
                CommunicationChannel.SMS,
                twilioProperties.getSmsFrom(),
                to,
                content
        );
    }

    public void sendWhatsApp(
            String externalOrderId,
            String to,
            ConfirmationMessageContent content
    ) {
        sendMessage(
                externalOrderId,
                CommunicationChannel.WHATSAPP,
                twilioProperties.getWhatsappFrom(),
                to,
                content
        );
    }

    private void sendMessage(
            String externalOrderId,
            CommunicationChannel channel,
            String from,
            String to,
            ConfirmationMessageContent content
    ) {
        validateProviderConfiguration(channel, from);
        validateRecipient(channel, to);

        String normalizedFrom = normalizeAddress(channel, from);
        String normalizedTo = normalizeAddress(channel, to);

        try {
            log.info(
                    "Sending Twilio notification. externalOrderId={}, channel={}, from={}, to={}, contentTemplateSidPresent={}",
                    externalOrderId,
                    channel,
                    maskAddress(normalizedFrom),
                    maskAddress(normalizedTo),
                    hasText(twilioProperties.getContentTemplateSid())
            );

            Message result = createMessage(
                    normalizedFrom,
                    normalizedTo,
                    content
            ).create(twilioRestClient);

            log.info(
                    "Twilio notification accepted. externalOrderId={}, channel={}, messageSid={}, status={}",
                    externalOrderId,
                    channel,
                    result.getSid(),
                    result.getStatus()
            );
        } catch (ApiException | JsonProcessingException ex) {
            log.error(
                    "Twilio delivery failed. externalOrderId={}, channel={}, from={}, to={}, contentTemplateSid={}, twilioCode={}, twilioStatus={}, possibleCause={}",
                    externalOrderId,
                    channel,
                    maskAddress(normalizedFrom),
                    maskAddress(normalizedTo),
                    twilioProperties.getContentTemplateSid(),
                    ex instanceof ApiException apiException ? apiException.getCode() : null,
                    ex instanceof ApiException apiException ? apiException.getStatusCode() : null,
                    determinePossibleCause(channel, normalizedFrom, normalizedTo),
                    ex
            );
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio notification could not be delivered for externalOrderId=" + externalOrderId,
                    ex
            );
        }
    }

    private Creator<Message> createMessage(
            String from,
            String to,
            ConfirmationMessageContent content
    ) throws JsonProcessingException {
        var messageCreator = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(from),
                (String) null
        );

        messageCreator.setContentSid(twilioProperties.getContentTemplateSid());
        messageCreator.setContentVariables(
                objectMapper.writeValueAsString(toContentVariables(content))
        );

        return messageCreator;
    }

    private Map<String, String> toContentVariables(ConfirmationMessageContent content) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", content.customerName());
        variables.put("2", content.externalOrderId());
        variables.put("3", content.product());
        variables.put("4", content.quantityLiters());
        variables.put("5", content.deliveryDate());
        variables.put("6", content.deliveryWindow());
        variables.put("7", content.deliveryAddress());
        variables.put("8", content.confirmationLink());
        return variables;
    }

    private void validateProviderConfiguration(
            CommunicationChannel channel,
            String from
    ) {
        if (!hasText(twilioProperties.getAccountSid())
                || !hasText(twilioProperties.getAuthToken())
                || !hasText(from)
                || !hasText(twilioProperties.getContentTemplateSid())) {
            log.error(
                    "Twilio configuration is incomplete. channel={}, hasAccountSid={}, hasAuthToken={}, hasFrom={}, hasContentTemplateSid={}, from={}",
                    channel,
                    hasText(twilioProperties.getAccountSid()),
                    hasText(twilioProperties.getAuthToken()),
                    hasText(from),
                    hasText(twilioProperties.getContentTemplateSid()),
                    maskAddress(from)
            );
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio configuration is incomplete for channel " + channel,
                    new IllegalStateException("Missing required Twilio properties.")
            );
        }

        if (!isE164(stripWhatsAppPrefix(from))) {
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio sender is not in valid E.164 format for channel " + channel,
                    new IllegalStateException("Invalid Twilio sender.")
            );
        }
    }

    private void validateRecipient(
            CommunicationChannel channel,
            String to
    ) {
        if (!hasText(to)) {
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio recipient is missing for channel " + channel,
                    new IllegalArgumentException("Missing recipient.")
            );
        }

        if (!isE164(stripWhatsAppPrefix(to))) {
            throw new NotificationDeliveryException(
                    channel,
                    "Twilio recipient is not in valid E.164 format for channel " + channel,
                    new IllegalArgumentException("Invalid recipient.")
            );
        }
    }

    private String normalizeAddress(
            CommunicationChannel channel,
            String address
    ) {
        String stripped = stripWhatsAppPrefix(address);
        return channel == CommunicationChannel.WHATSAPP
                ? WHATSAPP_PREFIX + stripped
                : stripped;
    }

    private String stripWhatsAppPrefix(String value) {
        return hasText(value) && value.startsWith(WHATSAPP_PREFIX)
                ? value.substring(WHATSAPP_PREFIX.length())
                : value;
    }

    private boolean isE164(String value) {
        return hasText(value) && E164_PATTERN.matcher(value).matches();
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
            return "shared content template may be unapproved for WhatsApp, not enabled for the sender, or the recipient may not be joined to the sandbox";
        }
        if (channel == CommunicationChannel.SMS && !to.startsWith("+")) {
            return "recipient SMS number is not in expected E.164 format";
        }
        return "check Twilio account credentials, shared content template provisioning, sender provisioning, and destination reachability";
    }

    private String maskAddress(String value) {
        if (!hasText(value)) {
            return "<empty>";
        }
        int visibleDigits = 4;
        int prefixLength = value.startsWith(WHATSAPP_PREFIX) ? WHATSAPP_PREFIX.length() : 0;
        String prefix = value.substring(0, prefixLength);
        String rest = value.substring(prefixLength);

        if (rest.length() <= visibleDigits) {
            return prefix + "****";
        }

        return prefix + "*".repeat(Math.max(0, rest.length() - visibleDigits))
                + rest.substring(rest.length() - visibleDigits);
    }
}
