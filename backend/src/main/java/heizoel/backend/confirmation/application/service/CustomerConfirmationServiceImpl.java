package heizoel.backend.confirmation.application.service;


import heizoel.backend.confirmation.application.port.out.DispoCallbackWorkflowService;
import heizoel.backend.confirmation.adapter.in.web.customer.dto.CustomerConfirmationPreviewDto;
import heizoel.backend.confirmation.application.port.in.CustomerConfirmationService;
import heizoel.backend.confirmation.application.port.out.CustomerResponseService;
import heizoel.backend.confirmation.domain.model.CustomerResponseType;
import heizoel.backend.confirmation.application.port.out.ConfirmationRequestService;
import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.confirmation.domain.exception.ConfirmationRequestNotFoundException;
import heizoel.backend.confirmation.domain.exception.CustomerResponseAlreadyExistsException;
import heizoel.backend.shared.exception.EmailSendingException;
import heizoel.backend.confirmation.application.port.out.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerConfirmationServiceImpl implements CustomerConfirmationService {

    private final ConfirmationRequestService confirmationRequestService;
    private final OrderSnapshotService orderSnapshotService;
    private final CustomerResponseService customerResponseService;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public CustomerConfirmationPreviewDto getConfirmationPreview(String token) {

        ConfirmationRequest confirmationRequest = confirmationRequestService.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));
        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        return new CustomerConfirmationPreviewDto(
                orderSnapshot.getExternalOrderId(),
                orderSnapshot.getCustomerName(),
                orderSnapshot.getDeliveryAddress(),
                orderSnapshot.getProduct(),
                orderSnapshot.getQuantityLiters(),
                confirmationRequest.getDeliveryDate(),
                confirmationRequest.getDeliveryWindowStart(),
                confirmationRequest.getDeliveryWindowEnd(),
                orderSnapshot.getPriceDisplayText(),
                orderSnapshot.getConfirmationStatus()
        );
    }


    @Override
    @Transactional
    public void confirm(String token, String customerComment) {
        submitCustomerResponse(
                token,
                CustomerResponseType.CONFIRM,
                ConfirmationStatus.CONFIRMED,
                customerComment
        );
    }

    @Override
    @Transactional
    public void reject(String token, String customerComment) {
        submitCustomerResponse(
                token,
                CustomerResponseType.REJECT,
                ConfirmationStatus.REJECTED,
                customerComment
        );
    }

    private void submitCustomerResponse(
            String token,
            CustomerResponseType responseType,
            ConfirmationStatus confirmationStatus,
            String customerComment
    ) {
        ConfirmationRequest confirmationRequest = findValidActiveRequest(token);

        if (customerResponseService.existsFor(confirmationRequest)) {
            throw new CustomerResponseAlreadyExistsException(
                    "A customer response already exists for this confirmation request."
            );
        }

        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        customerResponseService.create(
                confirmationRequest,
                responseType,
                customerComment
        );

        confirmationRequestService.markInactive(confirmationRequest);
        orderSnapshotService.updateStatus(orderSnapshot, confirmationStatus);

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
        ConfirmationRequest confirmationRequest = confirmationRequestService.findByToken(token)
                .orElseThrow(() -> new ConfirmationRequestNotFoundException(
                        "Confirmation request was not found."
                ));

        if (!confirmationRequest.isActive()) {
            throw new ConfirmationRequestInactiveException(
                    "This confirmation request is no longer active."
            );
        }

        if (confirmationRequest.getExpiresAt().isBefore(Instant.now())) {
            throw new ConfirmationRequestExpiredException(
                    "This confirmation request has expired."
            );
        }

        return confirmationRequest;
    }

}

