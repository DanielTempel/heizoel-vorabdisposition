package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.token.TokenService;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.model.CompanyContext;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.domain.model.Company;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.confirmation.domain.model.enumeration.CommunicationChannel;
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
class ConfirmationRequestPreparationServiceImplTest {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    @Mock
    ConfirmationRequestRepositoryPort confirmationRequestRepository;

    @Mock
    OrderSnapshotRepositoryPort orderSnapshotRepository;

    @Mock
    TokenService tokenService;

    @Mock
    Company company;

    @InjectMocks
    ConfirmationRequestPreparationServiceImpl service;

    @Test
    void create_capsExpirationAtDeliveryWindowStart() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(1);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);

        when(tokenService.generateToken()).thenReturn("token");
        when(company.getId()).thenReturn(1L);
        when(orderSnapshotRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderSnapshotRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequestCreationResult creationResult =
                service.prepareConfirmationRequest(
                company,
                command(deliveryDate, deliveryWindowStart, 168)
        );
        ConfirmationRequest result = creationResult.confirmationRequest();

        assertThat(result.getExpiresAt()).isEqualTo(
                deliveryDate.atTime(deliveryWindowStart).atZone(DELIVERY_ZONE).toInstant()
        );
        assertThat(result.getResponseDeadlineHours()).isEqualTo(168);
    }

    @Test
    void create_keepsRequestedExpirationWhenItIsBeforeDeliveryWindowStart() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);

        when(tokenService.generateToken()).thenReturn("token");
        when(company.getId()).thenReturn(1L);
        when(orderSnapshotRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderSnapshotRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequest result = service.prepareConfirmationRequest(
                company,
                command(deliveryDate, LocalTime.of(10, 0), 1)
        ).confirmationRequest();

        assertThat(result.getExpiresAt())
                .isEqualTo(result.getSentAt().plus(Duration.ofHours(1)));
    }

    @Test
    void create_rejectsDeliveryWindowThatAlreadyStarted() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).minusDays(1);

        when(company.getId()).thenReturn(1L);
        when(orderSnapshotRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderSnapshotRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.prepareConfirmationRequest(
                company,
                command(deliveryDate, LocalTime.of(10, 0), 24)
        ))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");

        verifyNoInteractions(tokenService);
        verify(confirmationRequestRepository, never()).save(any());
    }

    private CreateConfirmationRequestCommand command(
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return new CreateConfirmationRequestCommand(
                new CompanyContext(1L),
                "ORDER-1",
                "Customer",
                CommunicationChannel.EMAIL,
                "customer@example.com",
                null,
                "Address",
                "Heating oil",
                1000,
                deliveryDate,
                deliveryWindowStart,
                deliveryWindowStart.plusHours(1),
                responseDeadlineHours,
                "1,000 EUR"
        );
    }
}

