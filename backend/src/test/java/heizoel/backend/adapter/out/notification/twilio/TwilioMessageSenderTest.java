package heizoel.backend.adapter.out.notification.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import heizoel.backend.adapter.out.notification.ConfirmationMessageContent;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.configuration.properties.TwilioProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TwilioMessageSenderTest {

    @Test
    void sendWhatsAppRejectsInvalidRecipientAsNotificationDeliveryException() {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);

        assertThatThrownBy(() -> sender.sendWhatsApp("A-1024", "12345", content()))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("recipient")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendSmsRejectsMissingProviderConfigurationAsNotificationDeliveryException() {
        TwilioProperties properties = properties();
        properties.setAccountSid("");
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);

        assertThatThrownBy(() -> sender.sendSms("A-1024", "+491701234567", content()))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("configuration is incomplete")
                .hasCauseInstanceOf(IllegalStateException.class);

        verifyNoInteractions(objectMapper);
    }

    @Test
    void sendWhatsAppAddsPrefixToSenderAndRecipient() throws Exception {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);
        MessageCreator creator = mock(MessageCreator.class);
        Message result = mock(Message.class);

        when(objectMapper.writeValueAsString(Map.of(
                "1", "Max Mustermann",
                "2", "A-1024",
                "3", "Heizoel",
                "4", "3000",
                "5", "12.06.2099",
                "6", "10:00 - 11:00",
                "7", "Beispielstrasse 12, 97070 Wuerzburg",
                "8", "https://frontend.example/confirmation/token-123"
        ))).thenReturn("{\"1\":\"value\"}");
        when(creator.create(any(TwilioRestClient.class))).thenReturn(result);
        when(result.getSid()).thenReturn("SM123");
        when(result.getStatus()).thenReturn(Message.Status.QUEUED);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    eq(new PhoneNumber("whatsapp:+491701234567")),
                    eq(new PhoneNumber("whatsapp:+491709999999")),
                    eq((String) null)
            )).thenReturn(creator);

            sender.sendWhatsApp("A-1024", "+491701234567", content());
        }

        verify(creator).setContentSid("HX123");
        verify(creator).setContentVariables("{\"1\":\"value\"}");
    }

    @Test
    void sendWhatsAppDoesNotDuplicateExistingPrefix() throws Exception {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);
        MessageCreator creator = mock(MessageCreator.class);
        Message result = mock(Message.class);

        when(objectMapper.writeValueAsString(Map.of(
                "1", "Max Mustermann",
                "2", "A-1024",
                "3", "Heizoel",
                "4", "3000",
                "5", "12.06.2099",
                "6", "10:00 - 11:00",
                "7", "Beispielstrasse 12, 97070 Wuerzburg",
                "8", "https://frontend.example/confirmation/token-123"
        ))).thenReturn("{\"1\":\"value\"}");
        when(creator.create(any(TwilioRestClient.class))).thenReturn(result);
        when(result.getSid()).thenReturn("SM123");
        when(result.getStatus()).thenReturn(Message.Status.ACCEPTED);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    eq(new PhoneNumber("whatsapp:+491701234567")),
                    eq(new PhoneNumber("whatsapp:+491709999999")),
                    eq((String) null)
            )).thenReturn(creator);

            sender.sendWhatsApp("A-1024", "whatsapp:+491701234567", content());
        }

        verify(creator).setContentSid("HX123");
        verify(creator).setContentVariables("{\"1\":\"value\"}");
    }

    @Test
    void sendSmsUsesSharedContentSidAndSerializedVariables() throws Exception {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);
        MessageCreator creator = mock(MessageCreator.class);
        Message result = mock(Message.class);

        when(objectMapper.writeValueAsString(Map.of(
                "1", "Max Mustermann",
                "2", "A-1024",
                "3", "Heizoel",
                "4", "3000",
                "5", "12.06.2099",
                "6", "10:00 - 11:00",
                "7", "Beispielstrasse 12, 97070 Wuerzburg",
                "8", "https://frontend.example/confirmation/token-123"
        ))).thenReturn("{\"1\":\"value\"}");
        when(creator.create(any(TwilioRestClient.class))).thenReturn(result);
        when(result.getSid()).thenReturn("SM124");
        when(result.getStatus()).thenReturn(Message.Status.SENT);

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    eq(new PhoneNumber("+491701234567")),
                    eq(new PhoneNumber("+491709999999")),
                    eq((String) null)
            )).thenReturn(creator);

            sender.sendSms("A-1024", "+491701234567", content());
        }

        verify(creator).setContentSid("HX123");
        verify(creator).setContentVariables("{\"1\":\"value\"}");
        verify(result).getSid();
        verify(result).getStatus();
    }

    @Test
    void sendWhatsAppWrapsApiExceptionInNotificationDeliveryException() throws Exception {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioRestClient client = mock(TwilioRestClient.class);
        TwilioMessageSender sender = new TwilioMessageSender(client, properties, objectMapper);
        MessageCreator creator = mock(MessageCreator.class);

        when(objectMapper.writeValueAsString(Map.of(
                "1", "Max Mustermann",
                "2", "A-1024",
                "3", "Heizoel",
                "4", "3000",
                "5", "12.06.2099",
                "6", "10:00 - 11:00",
                "7", "Beispielstrasse 12, 97070 Wuerzburg",
                "8", "https://frontend.example/confirmation/token-123"
        ))).thenReturn("{\"1\":\"value\"}");
        when(creator.create(client)).thenThrow(new ApiException("boom"));

        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            mockedMessage.when(() -> Message.creator(
                    eq(new PhoneNumber("whatsapp:+491701234567")),
                    eq(new PhoneNumber("whatsapp:+491709999999")),
                    eq((String) null)
            )).thenReturn(creator);

            assertThatThrownBy(() -> sender.sendWhatsApp("A-1024", "+491701234567", content()))
                    .isInstanceOf(NotificationDeliveryException.class)
                    .hasMessageContaining("A-1024")
                    .hasCauseInstanceOf(ApiException.class);
        }
    }

    @Test
    void sendWhatsAppWrapsContentVariableSerializationFailureInNotificationDeliveryException() throws Exception {
        TwilioProperties properties = properties();
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        TwilioMessageSender sender = new TwilioMessageSender(mock(TwilioRestClient.class), properties, objectMapper);

        doThrow(new JsonProcessingException("boom") {
        }).when(objectMapper).writeValueAsString(Map.of(
                "1", "Max Mustermann",
                "2", "A-1024",
                "3", "Heizoel",
                "4", "3000",
                "5", "12.06.2099",
                "6", "10:00 - 11:00",
                "7", "Beispielstrasse 12, 97070 Wuerzburg",
                "8", "https://frontend.example/confirmation/token-123"
        ));

        assertThatThrownBy(() -> sender.sendWhatsApp("A-1024", "+491701234567", content()))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("A-1024")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    private ConfirmationMessageContent content() {
        return new ConfirmationMessageContent(
                "Max Mustermann",
                "A-1024",
                "Heizoel",
                "3000",
                "12.06.2099",
                "10:00 - 11:00",
                "Beispielstrasse 12, 97070 Wuerzburg",
                "https://frontend.example/confirmation/token-123"
        );
    }

    private TwilioProperties properties() {
        TwilioProperties properties = new TwilioProperties();
        properties.setAccountSid("AC123");
        properties.setAuthToken("token");
        properties.setSmsFrom("+491709999999");
        properties.setWhatsappFrom("+491709999999");
        properties.setContentTemplateSid("HX123");
        return properties;
    }
}
