package heizoel.backend.adapter.in.web.overview.dto;

import heizoel.backend.application.model.overview.OrderOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourResponseDtoTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 8, 5);

    @Test
    void mapsTourOrdersAndStatusCounts() {
        TourOverviewItem tour = new TourOverviewItem(
                "A-17",
                "WÜ-AB 123",
                DELIVERY_DATE,
                List.of(
                        order("SENT-1", ConfirmationStatus.SENT),
                        order("SENT-2", ConfirmationStatus.SENT),
                        order("CONFIRMED-1", ConfirmationStatus.CONFIRMED),
                        order("CONFIRMED-2", ConfirmationStatus.CONFIRMED),
                        order("CONFIRMED-3", ConfirmationStatus.CONFIRMED),
                        order("REJECTED-1", ConfirmationStatus.REJECTED),
                        order("NO-RESPONSE-1", ConfirmationStatus.NO_RESPONSE),
                        order("NO-RESPONSE-2", ConfirmationStatus.NO_RESPONSE)
                )
        );

        TourResponseDto result = TourResponseDto.from(tour);

        assertThat(result.tourNumber()).isEqualTo("A-17");
        assertThat(result.vehicleLicensePlate()).isEqualTo("WÜ-AB 123");
        assertThat(result.deliveryDate()).isEqualTo(DELIVERY_DATE);
        assertThat(result.statusCounts()).isEqualTo(
                new TourResponseDto.StatusCounts(2, 3, 1, 2)
        );
        assertThat(result.orders())
                .extracting(OrderResponseDto::externalOrderId)
                .containsExactly(
                        "SENT-1",
                        "SENT-2",
                        "CONFIRMED-1",
                        "CONFIRMED-2",
                        "CONFIRMED-3",
                        "REJECTED-1",
                        "NO-RESPONSE-1",
                        "NO-RESPONSE-2"
                );
        assertThat(result.orders().get(0)).isEqualTo(new OrderResponseDto(
                "SENT-1",
                "Customer SENT-1",
                "Address SENT-1",
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                CommunicationChannel.EMAIL,
                ConfirmationStatus.SENT,
                Instant.parse("2026-08-04T12:00:00Z")
        ));
    }

    @Test
    void countsOnlyOrdersPresentAfterFiltering() {
        TourOverviewItem filteredTour = new TourOverviewItem(
                "A-17",
                "WÜ-AB 123",
                DELIVERY_DATE,
                List.of(
                        order("REJECTED-1", ConfirmationStatus.REJECTED),
                        order("NO-RESPONSE-1", ConfirmationStatus.NO_RESPONSE),
                        order("NO-RESPONSE-2", ConfirmationStatus.NO_RESPONSE)
                )
        );

        TourResponseDto result = TourResponseDto.from(filteredTour);

        assertThat(result.statusCounts()).isEqualTo(
                new TourResponseDto.StatusCounts(0, 0, 1, 2)
        );
    }

    private OrderOverviewItem order(
            String externalOrderId,
            ConfirmationStatus status
    ) {
        return new OrderOverviewItem(
                externalOrderId,
                "Customer " + externalOrderId,
                "Address " + externalOrderId,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                CommunicationChannel.EMAIL,
                status,
                Instant.parse("2026-08-04T12:00:00Z")
        );
    }
}
