package heizoel.backend.adapter.out.notification.sms;

public record SmsSendRequestDto(
        String to,
        String text
) {
}
