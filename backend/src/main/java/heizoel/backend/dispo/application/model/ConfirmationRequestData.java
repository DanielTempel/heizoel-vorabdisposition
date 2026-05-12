package heizoel.backend.dispo.application.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmationRequestData (
        LocalDate deliveryDate,
        LocalTime deliveryWindowStart,
        LocalTime deliveryWindowEnd
) {
}