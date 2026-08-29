package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.application.exception.ConfirmationRequestResendNotAllowedException;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestCommand;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendConfirmationRequestServiceTest {

    private static final long COMPANY_ID = 1L;
    private static final String EXTERNAL_ORDER_ID = "ORDER-1";
    private static final Instant SENT_AT = Instant.parse("2099-06-10T10:00:00Z");
    private static final Instant NOW = Instant.parse("2099-06-11T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    OrderRepository orderRepository;

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    ConfirmationRequestStarter confirmationRequestStarter;

    ResendConfirmationRequestService service;

    @BeforeEach
    void setUp() {
        service = new ResendConfirmationRequestService(
                orderRepository,
                confirmationRequestRepository,
                confirmationRequestStarter,
                CLOCK
        );
    }

    @Test
    void resend_failedRequest_startsNewRequest() {
        Order order = spy(order("customer@example.com", "+491701234567"));
        DeliverySlot deliverySlot = futureDeliverySlot();
        ConfirmationRequest previousRequest = pendingRequest(order, deliverySlot);
        previousRequest.markDeliveryFailed();
        mockExisting(order, previousRequest);

        service.resend(command(CommunicationChannel.SMS, 48));

        verify(order).markOpen();
        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        verify(confirmationRequestStarter).createAndStart(
                order,
                CommunicationChannel.SMS,
                deliverySlot,
                48
        );
    }

    @Test
    void resend_noResponseRequest_startsNewRequest() {
        Order order = order("customer@example.com", "+491701234567");
        DeliverySlot deliverySlot = futureDeliverySlot();
        ConfirmationRequest previousRequest = sentRequest(order, deliverySlot);
        previousRequest.markInactive();
        order.markNoResponse();
        mockExisting(order, previousRequest);

        service.resend(command(CommunicationChannel.WHATSAPP, 36));

        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.OPEN);
        verify(confirmationRequestStarter).createAndStart(
                order,
                CommunicationChannel.WHATSAPP,
                deliverySlot,
                36
        );
    }

    @Test
    void resend_pendingRequest_isRejectedWithoutSideEffects() {
        Order order = spy(order("customer@example.com", "+491701234567"));
        ConfirmationRequest previousRequest = pendingRequest(order, futureDeliverySlot());

        assertResendRejectedWithoutSideEffects(order, previousRequest);
    }

    @Test
    void resend_activeSentRequest_isRejectedWithoutSideEffects() {
        Order order = spy(order("customer@example.com", "+491701234567"));
        ConfirmationRequest previousRequest = sentRequest(order, futureDeliverySlot());

        assertResendRejectedWithoutSideEffects(order, previousRequest);
    }

    @ParameterizedTest
    @EnumSource(
            value = ConfirmationStatus.class,
            names = {"CONFIRMED", "REJECTED"}
    )
    void resend_answeredRequest_isRejectedWithoutSideEffects(
            ConfirmationStatus status
    ) {
        Order order = spy(order("customer@example.com", "+491701234567"));
        ConfirmationRequest previousRequest = sentRequest(order, futureDeliverySlot());
        previousRequest.markInactive();
        if (status == ConfirmationStatus.CONFIRMED) {
            order.markConfirmed();
        } else {
            order.markRejected();
        }

        assertResendRejectedWithoutSideEffects(order, previousRequest);
    }

    @Test
    void resend_startedDeliveryWindow_isRejectedBeforeStateChanges() {
        Order order = spy(order("customer@example.com", "+491701234567"));
        ConfirmationRequest previousRequest = pendingRequest(
                order,
                DeliverySlot.of(
                        LocalDate.of(2099, 6, 11),
                        LocalTime.of(14, 0),
                        LocalTime.of(16, 0)
                )
        );
        previousRequest.markDeliveryFailed();
        ConfirmationStatus initialStatus = order.getConfirmationStatus();
        mockExisting(order, previousRequest);

        assertThatThrownBy(() -> service.resend(command(CommunicationChannel.EMAIL, 48)))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");

        verify(order, never()).markOpen();
        assertThat(order.getConfirmationStatus()).isEqualTo(initialStatus);
        verifyNoInteractions(confirmationRequestStarter);
    }

    @Test
    void resend_orderDoesNotExist_throwsOrderNotFound() {
        when(orderRepository.findByCompanyIdAndExternalOrderId(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resend(command(CommunicationChannel.EMAIL, 48)))
                .isInstanceOf(OrderNotFoundException.class);

        verifyNoInteractions(confirmationRequestRepository, confirmationRequestStarter);
    }

    @Test
    void resend_orderHasNoConfirmationRequest_throwsConfirmationRequestNotFound() {
        Order order = order("customer@example.com", "+491701234567");
        when(orderRepository.findByCompanyIdAndExternalOrderId(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resend(command(CommunicationChannel.EMAIL, 48)))
                .isInstanceOf(ConfirmationRequestNotFoundException.class);

        verifyNoInteractions(confirmationRequestStarter);
    }

    @Test
    void resend_emailWithoutCustomerEmail_throwsMissingDigitalContactBeforeRequestLookup() {
        Order order = order(null, "+491701234567");
        when(orderRepository.findByCompanyIdAndExternalOrderId(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.resend(command(CommunicationChannel.EMAIL, 48)))
                .isInstanceOf(MissingDigitalContactException.class);

        verifyNoInteractions(confirmationRequestRepository, confirmationRequestStarter);
    }

    @ParameterizedTest
    @EnumSource(
            value = CommunicationChannel.class,
            names = {"SMS", "WHATSAPP"}
    )
    void resend_phoneChannelWithoutPhone_throwsMissingDigitalContactBeforeRequestLookup(
            CommunicationChannel channel
    ) {
        Order order = order("customer@example.com", " ");
        when(orderRepository.findByCompanyIdAndExternalOrderId(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.resend(command(channel, 48)))
                .isInstanceOf(MissingDigitalContactException.class);

        verifyNoInteractions(confirmationRequestRepository, confirmationRequestStarter);
    }

    private void assertResendRejectedWithoutSideEffects(
            Order order,
            ConfirmationRequest previousRequest
    ) {
        ConfirmationStatus initialStatus = order.getConfirmationStatus();
        mockExisting(order, previousRequest);

        assertThatThrownBy(() -> service.resend(command(CommunicationChannel.EMAIL, 48)))
                .isInstanceOf(ConfirmationRequestResendNotAllowedException.class)
                .hasMessage("Confirmation request cannot be resent in the current state.");

        verify(order, never()).markOpen();
        assertThat(order.getConfirmationStatus()).isEqualTo(initialStatus);
        verifyNoInteractions(confirmationRequestStarter);
    }

    private void mockExisting(Order order, ConfirmationRequest request) {
        when(orderRepository.findByCompanyIdAndExternalOrderId(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.of(order));
        when(confirmationRequestRepository.findTopByOrderOrderByIdDesc(order))
                .thenReturn(Optional.of(request));
    }

    private ConfirmationRequest pendingRequest(Order order, DeliverySlot deliverySlot) {
        return ConfirmationRequest.createPending(
                order,
                "existing-token",
                CommunicationChannel.EMAIL,
                deliverySlot,
                24
        );
    }

    private ConfirmationRequest sentRequest(Order order, DeliverySlot deliverySlot) {
        ConfirmationRequest request = pendingRequest(order, deliverySlot);
        request.markSent(SENT_AT);
        order.markSent();
        return request;
    }

    private DeliverySlot futureDeliverySlot() {
        return DeliverySlot.of(
                LocalDate.of(2099, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
    }

    private ResendConfirmationRequestCommand command(
            CommunicationChannel channel,
            int responseDeadlineHours
    ) {
        return new ResendConfirmationRequestCommand(
                new CompanyContext(COMPANY_ID),
                EXTERNAL_ORDER_ID,
                channel,
                responseDeadlineHours
        );
    }

    private Order order(String customerEmail, String customerPhoneNumber) {
        return Order.create(
                Company.create("Company", "api-key-hash", "http://localhost/callback"),
                EXTERNAL_ORDER_ID,
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                customerEmail,
                customerPhoneNumber,
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        );
    }
}
