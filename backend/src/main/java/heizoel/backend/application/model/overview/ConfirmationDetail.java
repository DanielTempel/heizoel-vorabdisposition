package heizoel.backend.application.model.overview;

import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.NotificationDeliveryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ConfirmationDetail(
        OrderDetail order,
        RequestDetail currentRequest,
        List<RequestDetail> previousRequests
) {

    public ConfirmationDetail {
        previousRequests = List.copyOf(previousRequests);
    }

    public record OrderDetail(
            String externalOrderId,
            String customerName,
            String customerEmail,
            String customerPhoneNumber,
            String deliveryAddress,
            String product,
            Integer quantityLiters,
            String priceDisplayText,
            String tourNumber,
            String vehicleLicensePlate,
            ConfirmationStatus confirmationStatus
    ) {
    }

    public record RequestDetail(
            Long requestId,
            CommunicationChannel communicationChannel,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            LocalTime deliveryWindowEnd,
            Instant sentAt,
            Instant expiresAt,
            Integer responseDeadlineHours,
            boolean active,
            NotificationDeliveryStatus deliveryStatus,
            CustomerResponseDetail customerResponse
    ) {

        public String status() {
            return switch (deliveryStatus) {
                case PENDING -> NotificationDeliveryStatus.PENDING.name();
                case FAILED -> NotificationDeliveryStatus.FAILED.name();

                case SENT -> {
                    CustomerResponseType responseType =
                            customerResponse != null
                                    ? customerResponse.responseType()
                                    : null;

                    yield ConfirmationStatus.fromRequest(
                            active,
                            responseType
                    ).name();
                }
            };
        }
    }

    public record CustomerResponseDetail(
            CustomerResponseType responseType,
            String comment,
            Instant receivedAt
    ) {
    }
}