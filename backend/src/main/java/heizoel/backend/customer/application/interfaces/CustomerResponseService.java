package heizoel.backend.customer.application.interfaces;

import heizoel.backend.customer.domain.entity.CustomerResponse;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.customer.domain.CustomerResponseType;

public interface CustomerResponseService {


    boolean existsFor(ConfirmationRequest confirmationRequest);

    CustomerResponse create(
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType,
            String customerComment
    );
}

