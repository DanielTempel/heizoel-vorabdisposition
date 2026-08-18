package heizoel.backend.application.service.tracking;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.model.GeoCoordinate;
import heizoel.backend.application.port.in.tracking.DriverLocationResult;
import heizoel.backend.application.port.out.location.LocationTrackingService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDriverLocationServiceTest {

    private static final String TOKEN = "driver-location-token";

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    LocationTrackingService locationTrackingService;

    GetDriverLocationService service;

    @BeforeEach
    void setUp() {
        service = new GetDriverLocationService(
                confirmationRequestRepository,
                locationTrackingService
        );
    }

    @Test
    void rejectsOlderRequestTokenBeforeDriverLocationLogic() {
        when(confirmationRequestRepository.findLatestByToken(TOKEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDriverLocation(TOKEN))
                .isInstanceOf(ConfirmationRequestNotFoundException.class)
                .hasMessage("Confirmation request was not found.");

        verifyNoInteractions(locationTrackingService);
    }

    @Test
    void returnsExistingDriverLocationForLatestToken() {
        ConfirmationRequest latestRequest = requestForToday();
        GeoCoordinate coordinate = new GeoCoordinate(9.8820D, 49.8166D);
        when(confirmationRequestRepository.findLatestByToken(TOKEN))
                .thenReturn(Optional.of(latestRequest));
        when(locationTrackingService.getDriverLocation("ORDER-TRACKING"))
                .thenReturn(Optional.of(coordinate));

        Optional<DriverLocationResult> result = service.getDriverLocation(TOKEN);

        assertThat(result).get()
                .extracting(DriverLocationResult::coordinate)
                .isEqualTo(coordinate);
    }

    private ConfirmationRequest requestForToday() {
        return ConfirmationRequest.createPending(
                order(),
                TOKEN,
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.now(),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                ),
                24
        );
    }

    private Order order() {
        return Order.create(
                Company.create("Company", "api-key-hash", "http://localhost/callback"),
                "ORDER-TRACKING",
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                "customer@example.test",
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        );
    }
}
