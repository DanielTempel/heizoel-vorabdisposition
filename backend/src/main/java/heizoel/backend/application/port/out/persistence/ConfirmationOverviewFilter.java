package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.ConfirmationStatus;

import java.time.LocalDate;


public record ConfirmationOverviewFilter(
        Long companyId,
        LocalDate today,
        LocalDate deliveryDate,
        ConfirmationStatus status,
        String search
) {
}