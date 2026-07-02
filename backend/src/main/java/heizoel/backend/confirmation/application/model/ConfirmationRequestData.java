package heizoel.backend.confirmation.application.model;

import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmationRequestData (
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd,
        CommunicationChannel communicationChannel,
        Integer responseDeadline
) {
}
