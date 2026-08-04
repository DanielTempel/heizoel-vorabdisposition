package heizoel.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tour {

    @Column(name = "tour_number", nullable = false)
    private String tourNumber;

    @Column(name = "vehicle_license_plate", nullable = false, length = 50)
    private String vehicleLicensePlate;

    private Tour(
            String tourNumber,
            String vehicleLicensePlate
    ) {
        this.tourNumber = tourNumber;
        this.vehicleLicensePlate = vehicleLicensePlate;
    }

    public static Tour of(
            String tourNumber,
            String vehicleLicensePlate
    ) {
        return new Tour(tourNumber, vehicleLicensePlate);
    }

}