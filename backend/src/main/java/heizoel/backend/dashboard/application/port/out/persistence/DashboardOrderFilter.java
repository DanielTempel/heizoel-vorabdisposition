package heizoel.backend.dashboard.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;

import java.time.LocalDate;


public record DashboardOrderFilter(
        Long companyId,
        LocalDate today,
        LocalDate deliveryDate,
        ConfirmationStatus status,
        String search
) {
}