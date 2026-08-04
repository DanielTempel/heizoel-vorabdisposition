package heizoel.backend.application.service;

import heizoel.backend.application.port.out.token.TokenService;
import heizoel.backend.application.port.in.confirmation.ConfirmationRequestCreationResult;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.service.confirmation.ConfirmationRequestPreparationService;
import heizoel.backend.domain.*;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationRequestPreparationServiceTest {

    private static final ZoneId DELIVERY_ZONE = ZoneId.of("Europe/Berlin");

    @Spy
    Clock clock = Clock.system(DELIVERY_ZONE);

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    TokenService tokenService;

    @Mock
    Company company;

    @InjectMocks
    ConfirmationRequestPreparationService service;

    @Test
    void prepare_createsOrderAndRequestWhenOrderDoesNotExist() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);

        when(tokenService.generateToken()).thenReturn("token");
        when(company.getId()).thenReturn(1L);
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequestCreationResult result = service.prepareConfirmationRequest(
                company,
                command(deliveryDate, deliveryWindowStart, 24)
        );

        assertThat(result.created()).isTrue();
        assertThat(result.order().getExternalOrderId()).isEqualTo("ORDER-1");
        assertThat(result.order().getTour()).isEqualTo(Tour.of("17", "WÜ-AB 123"));
        assertThat(result.confirmationRequest().getOrder()).isSameAs(result.order());
        verify(orderRepository).save(result.order());
        verify(confirmationRequestRepository).save(result.confirmationRequest());
    }

    @Test
    void create_capsExpirationAtDeliveryWindowStart() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(1);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);

        when(tokenService.generateToken()).thenReturn("token");
        when(company.getId()).thenReturn(1L);
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderRepository.save(any()))
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
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderRepository.save(any()))
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
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(java.util.Optional.empty());
        when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.prepareConfirmationRequest(
                company,
                command(deliveryDate, LocalTime.of(10, 0), 24)
        ))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");

        verify(tokenService).generateToken();
        verify(confirmationRequestRepository, never()).save(any());
    }

    @Test
    void prepare_reusesRequestWhenTourIsIdentical() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        mockExistingOrder(order, latestRequest);

        ConfirmationRequestCreationResult result = service.prepareConfirmationRequest(
                company,
                command(
                        "17",
                        "WÜ-AB 123",
                        deliveryDate,
                        deliveryWindowStart,
                        24
                )
        );

        assertThat(result.created()).isFalse();
        assertThat(result.confirmationRequest()).isSameAs(latestRequest);
        assertThat(latestRequest.isActive()).isTrue();
        verify(orderRepository, never()).save(any());
        verify(confirmationRequestRepository, never()).save(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void prepare_preservesConfirmedResponseWhenDataIsUnchanged() {
        assertCompletedResponseIsReused(ConfirmationStatus.CONFIRMED);
    }

    @Test
    void prepare_preservesRejectedResponseWhenDataIsUnchanged() {
        assertCompletedResponseIsReused(ConfirmationStatus.REJECTED);
    }

    @Test
    void prepare_createsNewRequestAfterNoResponseWhenDataIsUnchanged() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        order.markNoResponse();
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );
        latestRequest.markInactive();

        ConfirmationRequestCreationResult result = prepareChangedRequest(
                order,
                latestRequest,
                command(deliveryDate, deliveryWindowStart, 24)
        );

        assertThat(result.created()).isTrue();
        assertThat(result.confirmationRequest()).isNotSameAs(latestRequest);
        assertThat(result.order().getConfirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        verify(confirmationRequestRepository, never()).save(latestRequest);
    }

    @Test
    void prepare_createsNewRequestWhenDeliverySlotChanges() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        ConfirmationRequestCreationResult result = prepareChangedRequest(
                order,
                latestRequest,
                command(deliveryDate, deliveryWindowStart.plusHours(1), 24)
        );

        assertNewRequestReplaced(latestRequest, result);
    }

    @Test
    void prepare_createsNewRequestWhenCommunicationChannelChanges() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        ConfirmationRequestCreationResult result = prepareChangedRequest(
                order,
                latestRequest,
                command(
                        "17",
                        "WÜ-AB 123",
                        "Customer",
                        CommunicationChannel.SMS,
                        deliveryDate,
                        deliveryWindowStart,
                        24
                )
        );

        assertNewRequestReplaced(latestRequest, result);
        assertThat(result.confirmationRequest().getCommunicationChannel())
                .isEqualTo(CommunicationChannel.SMS);
    }

    @Test
    void prepare_createsNewRequestWhenResponseDeadlineChanges() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        ConfirmationRequestCreationResult result = prepareChangedRequest(
                order,
                latestRequest,
                command(deliveryDate, deliveryWindowStart, 48)
        );

        assertNewRequestReplaced(latestRequest, result);
        assertThat(result.confirmationRequest().getResponseDeadlineHours()).isEqualTo(48);
    }

    @Test
    void prepare_updatesOrderAndCreatesNewRequestWhenOrderDataChanges() {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        ConfirmationRequestCreationResult result = prepareChangedRequest(
                order,
                latestRequest,
                command(
                        "17",
                        "WÜ-AB 123",
                        "Changed Customer",
                        CommunicationChannel.EMAIL,
                        deliveryDate,
                        deliveryWindowStart,
                        24
                )
        );

        assertNewRequestReplaced(latestRequest, result);
        assertThat(result.order().getCustomerName()).isEqualTo("Changed Customer");
        verify(orderRepository).save(order);
    }

    @Test
    void prepare_createsNewRequestWhenTourNumberChanges() {
        assertTourChangeCreatesNewRequest(
                Tour.of("17", "WÜ-AB 123"),
                "18",
                "WÜ-AB 123"
        );
    }

    @Test
    void prepare_createsNewRequestWhenVehicleLicensePlateChanges() {
        assertTourChangeCreatesNewRequest(
                Tour.of("17", "WÜ-AB 123"),
                "17",
                "WÜ-CD 456"
        );
    }

    private void assertCompletedResponseIsReused(ConfirmationStatus status) {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(Tour.of("17", "WÜ-AB 123"));
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );
        latestRequest.markInactive();
        if (status == ConfirmationStatus.CONFIRMED) {
            order.markConfirmed();
        } else {
            order.markRejected();
        }

        mockExistingOrder(order, latestRequest);

        ConfirmationRequestCreationResult result = service.prepareConfirmationRequest(
                company,
                command(deliveryDate, deliveryWindowStart, 24)
        );

        assertThat(result.created()).isFalse();
        assertThat(result.confirmationRequest()).isSameAs(latestRequest);
        assertThat(result.order().getConfirmationStatus()).isEqualTo(status);
        verify(orderRepository, never()).save(any());
        verify(confirmationRequestRepository, never()).save(any());
        verifyNoInteractions(tokenService);
    }

    private ConfirmationRequestCreationResult prepareChangedRequest(
            Order order,
            ConfirmationRequest latestRequest,
            CreateConfirmationRequestCommand command
    ) {
        mockExistingOrder(order, latestRequest);
        when(tokenService.generateToken()).thenReturn("new-token");
        when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return service.prepareConfirmationRequest(company, command);
    }

    private void assertNewRequestReplaced(
            ConfirmationRequest latestRequest,
            ConfirmationRequestCreationResult result
    ) {
        assertThat(result.created()).isTrue();
        assertThat(result.confirmationRequest()).isNotSameAs(latestRequest);
        assertThat(latestRequest.isActive()).isFalse();
        verify(confirmationRequestRepository).save(latestRequest);
        verify(confirmationRequestRepository).save(result.confirmationRequest());
    }

    private void assertTourChangeCreatesNewRequest(
            Tour existingTour,
            String requestedTourNumber,
            String requestedVehicleLicensePlate
    ) {
        LocalDate deliveryDate = LocalDate.now(DELIVERY_ZONE).plusDays(7);
        LocalTime deliveryWindowStart = LocalTime.of(10, 0);
        Order order = order(existingTour);
        ConfirmationRequest latestRequest = confirmationRequest(
                order,
                deliveryDate,
                deliveryWindowStart
        );

        mockExistingOrder(order, latestRequest);
        when(tokenService.generateToken()).thenReturn("new-token");
        when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationRequestCreationResult result = service.prepareConfirmationRequest(
                company,
                command(
                        requestedTourNumber,
                        requestedVehicleLicensePlate,
                        deliveryDate,
                        deliveryWindowStart,
                        24
                )
        );

        assertThat(result.created()).isTrue();
        assertThat(result.confirmationRequest()).isNotSameAs(latestRequest);
        assertThat(latestRequest.isActive()).isFalse();
        assertThat(result.order().getTour()).isEqualTo(
                Tour.of(requestedTourNumber, requestedVehicleLicensePlate)
        );
        verify(orderRepository).save(order);
        verify(confirmationRequestRepository).save(latestRequest);
        verify(confirmationRequestRepository).save(result.confirmationRequest());
    }

    private void mockExistingOrder(
            Order order,
            ConfirmationRequest latestRequest
    ) {
        when(company.getId()).thenReturn(1L);
        when(orderRepository.findByCompanyIdAndExternalOrderId(1L, "ORDER-1"))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.of(latestRequest));
    }

    private Order order(Tour tour) {
        return Order.create(
                company,
                "ORDER-1",
                tour,
                "Customer",
                "customer@example.com",
                null,
                "Address",
                "Heating oil",
                1000,
                "1,000 EUR"
        );
    }

    private ConfirmationRequest confirmationRequest(
            Order order,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart
    ) {
        return ConfirmationRequest.create(
                order,
                "existing-token",
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        deliveryDate,
                        deliveryWindowStart,
                        deliveryWindowStart.plusHours(1)
                ),
                Instant.now(clock),
                24
        );
    }

    private CreateConfirmationRequestCommand command(
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return command(
                "17",
                "WÜ-AB 123",
                deliveryDate,
                deliveryWindowStart,
                responseDeadlineHours
        );
    }

    private CreateConfirmationRequestCommand command(
            String tourNumber,
            String vehicleLicensePlate,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return command(
                tourNumber,
                vehicleLicensePlate,
                "Customer",
                CommunicationChannel.EMAIL,
                deliveryDate,
                deliveryWindowStart,
                responseDeadlineHours
        );
    }

    private CreateConfirmationRequestCommand command(
            String tourNumber,
            String vehicleLicensePlate,
            String customerName,
            CommunicationChannel communicationChannel,
            LocalDate deliveryDate,
            LocalTime deliveryWindowStart,
            int responseDeadlineHours
    ) {
        return new CreateConfirmationRequestCommand(
                new CompanyContext(1L),
                "ORDER-1",
                tourNumber,
                vehicleLicensePlate,
                customerName,
                communicationChannel,
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

