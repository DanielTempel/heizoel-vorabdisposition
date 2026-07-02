package heizoel.backend.confirmation.adapter.notification.whatsapp;

public record WhatsAppSendRequestDto(
        String to,
        String text
) {
}
