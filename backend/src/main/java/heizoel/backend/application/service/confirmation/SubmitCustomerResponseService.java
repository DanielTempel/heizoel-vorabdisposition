package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseUseCase;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
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
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional
    public void submitCustomerResponse(SubmitCustomerResponseCommand command) {
        ConfirmationStatus confirmationStatus = switch (command.responseType()) {
            case CONFIRM -> ConfirmationStatus.CONFIRMED;
            case REJECT -> ConfirmationStatus.REJECTED;
        };

        submitCustomerResponse(
                command.token(),
                command.responseType(),
                confirmationStatus,
                command.customerComment()
        );
    }

    private void submitCustomerResponse(
            String token,
            CustomerResponseType responseType,
            ConfirmationStatus confirmationStatus,
            String customerComment
    ) {
        ConfirmationRequest confirmationRequest = findValidActiveRequest(token);

        if (customerResponseRepository.existsByConfirmationRequest(confirmationRequest)) {
            throw new CustomerResponseAlreadyExistsException(
                    "A customer response already exists for this confirmation request."
            );
        }

        Order order = confirmationRequest.getOrder();

        CustomerResponse customerResponse = CustomerResponse.create(
                confirmationRequest,
                responseType,
                customerComment,
                Instant.now(clock)
        );
        customerResponseRepository.save(customerResponse);

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);
        switch (confirmationStatus) {
            case CONFIRMED -> order.markConfirmed();
            case REJECTED -> order.markRejected();
            default -> throw new IllegalArgumentException(
                    "Unsupported customer response status: " + confirmationStatus
            );
        }
        orderRepository.save(order);

        try {
            notificationService.sendCustomerResponseReceived(
                    order,
                    confirmationRequest,
                    responseType
            );
        } catch (NotificationDeliveryException ex) {
            log.warn(
                    "Customer response follow-up notification could not be delivered. externalOrderId={}, responseType={}, channel={}",
                    order.getExternalOrderId(),
                    responseType,
                    ex.getChannel(),
                    ex
            );
        }

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                order.getId(),
                confirmationStatus,
                customerComment
        );
    }

    private ConfirmationRequest findValidActiveRequest(String token) {
        ConfirmationRequest confirmationRequest = confirmationRequestRepository.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));

        if (!confirmationRequest.isActive()) {
            throw new ConfirmationRequestInactiveException(
                    "This confirmation request is no longer active."
            );
        }

        if (confirmationRequest.isExpiredAt(Instant.now(clock))) {
            throw new ConfirmationRequestExpiredException(
                    "This confirmation request has expired."
            );
        }

        return confirmationRequest;
    }
}
