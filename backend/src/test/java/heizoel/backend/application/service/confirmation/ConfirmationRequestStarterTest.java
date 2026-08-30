package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.application.port.out.token.TokenService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import heizoel.backend.domain.company.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationRequestStarterTest {

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    TokenService tokenService;

    @Mock
    ConfirmationWorkflowService confirmationWorkflowService;

    @Mock
    ConfirmationRequest savedRequest;

    @Test
    void createsPendingRequestAndStartsWorkflowWithSavedId() {
        ConfirmationRequestStarter starter = new ConfirmationRequestStarter(
                confirmationRequestRepository,
                tokenService,
                confirmationWorkflowService
        );
        Order order = order();
        DeliverySlot deliverySlot = DeliverySlot.of(
                LocalDate.of(2099, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );
        when(tokenService.generateToken()).thenReturn("generated-token");
        when(confirmationRequestRepository.save(any(ConfirmationRequest.class)))
                .thenReturn(savedRequest);
        when(savedRequest.getId()).thenReturn(41L);

        starter.createAndStart(
                order,
                CommunicationChannel.SMS,
                deliverySlot,
                48
        );

        ArgumentCaptor<ConfirmationRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationRequestRepository).save(requestCaptor.capture());
        ConfirmationRequest request = requestCaptor.getValue();
        assertThat(request.getOrder()).isSameAs(order);
        assertThat(request.getToken()).isEqualTo("generated-token");
        assertThat(request.getCommunicationChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(request.getDeliverySlot()).isEqualTo(deliverySlot);
        assertThat(request.getResponseDeadlineHours()).isEqualTo(48);
        assertThat(request.isPending()).isTrue();
        assertThat(request.isActive()).isFalse();
        assertThat(request.getSentAt()).isNull();
        assertThat(request.getExpiresAt()).isNull();
        verify(confirmationWorkflowService).startDeliveryProcess(41L);
    }

    private Order order() {
        return Order.create(
                Company.create(
                        "Company",
                        "api-key-hash",
                        "http://localhost/callback"
                ),
                "ORDER-START",
                Tour.of("17", "WUE-AB 123"),
                "Customer",
                null,
                "+491701234567",
                "Address",
                "Heating oil",
                1_000,
                "1,000 EUR"
        );
    }
}
