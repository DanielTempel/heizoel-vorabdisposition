package heizoel.backend.application.service.confirmation;

import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseCommand;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseUseCase;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.application.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.OrderSnapshot;
import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.adapter.out.persistence.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitCustomerResponseService implements SubmitCustomerResponseUseCase {

    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final OrderSnapshotRepository orderSnapshotRepository;
    private final CustomerResponseRepository customerResponseRepository;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;
    private final NotificationService notificationService;

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

        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        CustomerResponse customerResponse = CustomerResponse.create(
                confirmationRequest,
                responseType,
                customerComment,
                Instant.now()
        );
        customerResponseRepository.save(customerResponse);

        confirmationRequest.markInactive();
        confirmationRequestRepository.save(confirmationRequest);
        switch (confirmationStatus) {
            case CONFIRMED -> orderSnapshot.markConfirmed();
            case REJECTED -> orderSnapshot.markRejected();
            default -> throw new IllegalArgumentException(
                    "Unsupported customer response status: " + confirmationStatus
            );
        }
        orderSnapshotRepository.save(orderSnapshot);

        try {
            notificationService.sendCustomerResponseReceived(
                    orderSnapshot,
                    confirmationRequest,
                    responseType
            );
        } catch (NotificationDeliveryException ex) {
            log.warn(
                    "Customer response follow-up notification could not be delivered. externalOrderId={}, responseType={}, channel={}",
                    orderSnapshot.getExternalOrderId(),
                    responseType,
                    ex.getChannel(),
                    ex
            );
        }

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                orderSnapshot.getId(),
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

        if (confirmationRequest.isExpiredAt(Instant.now())) {
            throw new ConfirmationRequestExpiredException(
                    "This confirmation request has expired."
            );
        }

        return confirmationRequest;
    }
}
