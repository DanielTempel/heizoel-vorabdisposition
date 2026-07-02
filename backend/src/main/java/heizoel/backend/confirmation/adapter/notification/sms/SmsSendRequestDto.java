package heizoel.backend.confirmation.adapter.notification.sms;

public record SmsSendRequestDto(
        String to,
        String text
) {
}
