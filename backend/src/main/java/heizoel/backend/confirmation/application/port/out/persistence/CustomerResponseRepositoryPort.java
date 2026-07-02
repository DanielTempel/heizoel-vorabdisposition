package heizoel.backend.confirmation.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponse;

public interface CustomerResponseRepositoryPort {

    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);

    CustomerResponse save(CustomerResponse customerResponse);
}
