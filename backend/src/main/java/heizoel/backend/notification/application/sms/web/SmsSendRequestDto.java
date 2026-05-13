package heizoel.backend.notification.application.sms.web;

public record SmsSendRequestDto(
        String to,
        String text
) {
}