package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseUseCase;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.ConfirmationWorkflowService;
import heizoel.backend.domain.*;
import heizoel.backend.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitCustomerResponseService implements SubmitCustomerResponseUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final OrderRepository orderRepository;
    private final CustomerResponseRepository customerResponseRepository;

    private final ConfirmationWorkflowService confirmationWorkflowService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional
    public void submitCustomerResponse(SubmitCustomerResponseCommand command) {

        Order order = orderRepository
                .findByConfirmationRequestTokenForUpdate(command.token())
                .orElseThrow(() ->
                        new ConfirmationRequestNotFoundException(
                                "Confirmation request was not found."
                        )
                );

        ConfirmationRequest request =
                confirmationRequestRepository
                        .findByToken(command.token())
                        .orElseThrow(() ->
                                new ConfirmationRequestNotFoundException(
                                        "Confirmation request was not found."
                                )
                        );

        Instant receivedAt = Instant.now(clock);

        if (!request.isActive()) {
            throw new ConfirmationRequestInactiveException(
                    "This confirmation request is no longer active."
            );
        }

        if (request.isExpiredAt(receivedAt)) {
            throw new ConfirmationRequestExpiredException(
                    "This confirmation request has expired."
            );
        }

        if (customerResponseRepository.existsByConfirmationRequest(request)) {
            throw new CustomerResponseAlreadyExistsException(
                    "A customer response already exists for this confirmation request."
            );
        }

        ConfirmationStatus confirmationStatus =
                switch (command.responseType()) {
                    case CONFIRM -> {
                        order.markConfirmed();
                        yield ConfirmationStatus.CONFIRMED;
                    }

                    case REJECT -> {
                        order.markRejected();
                        yield ConfirmationStatus.REJECTED;
                    }
                };

        CustomerResponse customerResponse =
                CustomerResponse.create(
                        request,
                        command.responseType(),
                        command.customerComment(),
                        receivedAt
                );

        customerResponseRepository.save(customerResponse);

        request.markInactive();

        confirmationWorkflowService
                .notifyCustomerResponseReceived(
                        request.getId(),
                        order.getId(),
                        confirmationStatus,
                        command.customerComment()
                );

        try {
            notificationService.sendCustomerResponseReceived(
                    order,
                    request,
                    command.responseType()
            );
        } catch (NotificationDeliveryException exception) {
            log.warn(
                    "Customer response follow-up notification could not be delivered. "
                            + "externalOrderId={}, responseType={}, channel={}",
                    order.getExternalOrderId(),
                    command.responseType(),
                    exception.getChannel(),
                    exception
            );
        }
    }
}


