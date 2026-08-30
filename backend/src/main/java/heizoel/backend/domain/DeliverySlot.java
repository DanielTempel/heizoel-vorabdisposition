package heizoel.backend.domain;


import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliverySlot {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    @Column(name = "delivery_date", nullable = false)
    private LocalDate date;

    @Column(name = "delivery_window_start", nullable = false)
    private LocalTime start;

    @Column(name = "delivery_window_end", nullable = false)
    private LocalTime end;

    private DeliverySlot(
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
        if (date == null) {
            throw new InvalidDeliveryWindowException(
                    "Delivery date is required."
            );
        }

        if (start == null || end == null) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start and end are required."
            );
        }

        if (!start.isBefore(end)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }

        this.date = date;
        this.start = start;
        this.end = end;
    }

    public static DeliverySlot of(
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
        return new DeliverySlot(date, start, end);
    }

    public Instant startsAt() {
        return date
                .atTime(start)
                .atZone(DELIVERY_ZONE)
                .toInstant();
    }

    public void validateStartsAfter(Instant referenceTime) {
        if (!startsAt().isAfter(referenceTime)) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window must start in the future."
            );
        }
    }

}
