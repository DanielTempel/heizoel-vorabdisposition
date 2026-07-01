package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.domain.model.CustomerResponse;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponseType;

public interface CustomerResponseService {


    boolean existsFor(ConfirmationRequest confirmationRequest);

    CustomerResponse create(
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType,
            String customerComment
    );
}


