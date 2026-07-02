package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.customer.SubmitCustomerResponseCommand;
import heizoel.backend.confirmation.application.port.in.customer.SubmitCustomerResponseUseCase;
import heizoel.backend.confirmation.application.port.out.notification.NotificationService;
import heizoel.backend.confirmation.application.port.out.persistence.ConfirmationRequestRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.confirmation.application.port.out.workflow.DispoCallbackWorkflowService;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.confirmation.domain.exception.CustomerResponseAlreadyExistsException;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponse;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.shared.exception.EmailSendingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitCustomerResponseUseCaseImpl implements SubmitCustomerResponseUseCase {

    private final ConfirmationRequestRepositoryPort confirmationRequestRepository;
    private final OrderSnapshotRepositoryPort orderSnapshotRepository;
    private final CustomerResponseRepositoryPort customerResponseRepository;
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
        } catch (EmailSendingException ex) {
            log.warn(
                    "Customer response follow-up e-mail could not be sent. externalOrderId={}, responseType={}",
                    orderSnapshot.getExternalOrderId(),
                    responseType,
                    ex
            );
        }

        dispoCallbackWorkflowService.startDispoCallbackProcess(
                orderSnapshot.getExternalOrderId(),
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
