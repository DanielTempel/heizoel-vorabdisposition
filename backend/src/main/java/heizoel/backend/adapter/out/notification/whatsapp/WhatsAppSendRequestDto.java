package heizoel.backend.adapter.out.notification.whatsapp;

public record WhatsAppSendRequestDto(
        String to,
        String text
) {
}
