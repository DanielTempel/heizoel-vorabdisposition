package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.TokenService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.confirmation.domain.model.CommunicationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationRequestServiceImplTest {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    TokenService tokenService;

    @InjectMocks
    ConfirmationRequestServiceImpl service;

    @Test
    void create_capsExpirationAtDeliveryWindowStart() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(1);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);

        when(tokenService.generateToken()).thenReturn("token");
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequest result = service.create(
                new OrderSnapshot(),
                requestData(deliveryDate, deliveryWindowStart, 168)
        );

        assertThat(result.getExpiresAt()).isEqualTo(
                deliveryDate.atTime(deliveryWindowStart).atZone(DELIVERY_ZONE).toInstant()
        );
        assertThat(result.getResponseDeadlineHours()).isEqualTo(168);
    }

    @Test
    void create_keepsRequestedExpirationWhenItIsBeforeDeliveryWindowStart() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);

        when(tokenService.generateToken()).thenReturn("token");
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequest result = service.create(
                new OrderSnapshot(),
                requestData(deliveryDate, LocalTime.of(10, 0), 1)
        );

        assertThat(result.getExpiresAt())
                .isEqualTo(result.getSentAt().plus(Duration.ofHours(1)));
    }

    @Test
    void create_rejectsDeliveryWindowThatAlreadyStarted() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).minusDays(1);

        assertThatThrownBy(() -> service.create(
                new OrderSnapshot(),
                requestData(deliveryDate, LocalTime.of(10, 0), 24)
        ))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");

        verifyNoInteractions(tokenService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    private ConfirmationRequestData requestData(
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return new ConfirmationRequestData(
                deliveryDate,
                deliveryWindowStart,
                deliveryWindowStart.plusHours(1),
                CommunicationChannel.EMAIL,
                responseDeadlineHours
        );
    }
}

