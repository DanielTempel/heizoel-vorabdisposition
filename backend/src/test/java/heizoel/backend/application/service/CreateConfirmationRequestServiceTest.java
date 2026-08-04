package heizoel.backend.application.service;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.confirmation.ConfirmationRequestCreationResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.NoResponseWorkflowService;
import heizoel.backend.application.service.confirmation.ConfirmationRequestPreparationService;
import heizoel.backend.application.service.confirmation.CreateConfirmationRequestService;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateConfirmationRequestServiceTest {

    @Mock
    CompanyRepository companyRepository;

    @Mock
    ConfirmationRequestPreparationService confirmationRequestPreparationService;

    @Mock
    NotificationService notificationService;

    @Mock
    NoResponseWorkflowService noResponseWorkflowService;

    @Mock
    Company company;

    @Mock
    Order order;

    @Mock
    ConfirmationRequest confirmationRequest;

    @InjectMocks
    CreateConfirmationRequestService service;

    @Test
    void create_sendsMessageAndStartsTimeoutWhenRequestWasCreated() {
        CreateConfirmationRequestCommand command = command();
        Instant expiresAt = Instant.parse("2099-06-11T10:00:00Z");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(confirmationRequestPreparationService.prepareConfirmationRequest(company, command))
                .thenReturn(new ConfirmationRequestCreationResult(
                        order,
                        confirmationRequest,
                        true
                ));
        when(order.getExternalOrderId()).thenReturn("ORDER-1");
        when(order.getConfirmationStatus()).thenReturn(ConfirmationStatus.SENT);
        when(confirmationRequest.getId()).thenReturn(42L);
        when(confirmationRequest.getExpiresAt()).thenReturn(expiresAt);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.created()).isTrue();
        verify(notificationService).sendConfirmationRequest(order, confirmationRequest);
        verify(noResponseWorkflowService).startTimeoutProcess(42L, expiresAt);
    }

    @Test
    void create_doesNotSendMessageOrStartTimeoutWhenRequestWasReused() {
        CreateConfirmationRequestCommand command = command();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(confirmationRequestPreparationService.prepareConfirmationRequest(company, command))
                .thenReturn(new ConfirmationRequestCreationResult(
                        order,
                        confirmationRequest,
                        false
                ));
        when(order.getExternalOrderId()).thenReturn("ORDER-1");
        when(order.getConfirmationStatus()).thenReturn(ConfirmationStatus.CONFIRMED);

        CreateConfirmationRequestResult result = service.createConfirmationRequest(command);

        assertThat(result.created()).isFalse();
        verifyNoInteractions(notificationService, noResponseWorkflowService);
    }

    private CreateConfirmationRequestCommand command() {
        return new CreateConfirmationRequestCommand(
                new CompanyContext(1L),
                "ORDER-1",
                "17",
                "WÜ-AB 123",
                "Customer",
                CommunicationChannel.EMAIL,
                "customer@example.com",
                null,
                "Address",
                "Heating oil",
                1000,
                LocalDate.of(2099, 6, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                24,
                "1,000 EUR"
        );
    }
}
