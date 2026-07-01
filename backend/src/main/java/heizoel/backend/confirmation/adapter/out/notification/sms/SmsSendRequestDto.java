package heizoel.backend.confirmation.adapter.out.notification.sms;

public record SmsSendRequestDto(
        String to,
        String text
) {
}
