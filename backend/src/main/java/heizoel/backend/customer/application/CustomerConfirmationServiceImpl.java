package heizoel.backend.customer.application;


import heizoel.backend.camunda.application.interfaces.DispoCallbackWorkflowService;
import heizoel.backend.customer.api.dto.CustomerConfirmationPreviewDto;
import heizoel.backend.customer.application.interfaces.CustomerConfirmationService;
import heizoel.backend.customer.application.interfaces.CustomerResponseService;
import heizoel.backend.customer.domain.CustomerResponseType;
import heizoel.backend.dispo.application.interfaces.ConfirmationRequestService;
import heizoel.backend.dispo.application.interfaces.OrderSnapshotService;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.exceptions.customer.ConfirmationRequestExpiredException;
import heizoel.backend.exceptions.customer.ConfirmationRequestInactiveException;
import heizoel.backend.exceptions.customer.ConfirmationRequestNotFoundException;
import heizoel.backend.exceptions.customer.CustomerResponseAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerConfirmationServiceImpl implements CustomerConfirmationService {

    private final ConfirmationRequestService confirmationRequestService;
    private final OrderSnapshotService orderSnapshotService;
    private final CustomerResponseService customerResponseService;
    private final DispoCallbackWorkflowService dispoCallbackWorkflowService;

    @Override
    @Transactional(readOnly = true)
    public CustomerConfirmationPreviewDto getConfirmationPreview(String token) {

        ConfirmationRequest confirmationRequest = findValidActiveRequest(token);
        OrderSnapshot orderSnapshot = confirmationRequest.getOrderSnapshot();

        return new CustomerConfirmationPreviewDto(
                orderSnapshot.getExternalOrderId(),
                orderSnapshot.getCustomerName(),
                orderSnapshot.getDeliveryAddress(),
                orderSnapshot.getProduct(),
                orderSnapshot.getQuantityLiters(),
                confirmationRequest.getDeliveryDate(),
                confirmationRequest.getDeliveryWindowStart(),
                confirmationRequest.getDeliveryWindowEnd()
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
