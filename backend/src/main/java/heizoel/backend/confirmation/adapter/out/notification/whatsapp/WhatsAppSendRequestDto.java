package heizoel.backend.confirmation.adapter.out.notification.whatsapp;

public record WhatsAppSendRequestDto(
        String to,
        String text
) {
}
