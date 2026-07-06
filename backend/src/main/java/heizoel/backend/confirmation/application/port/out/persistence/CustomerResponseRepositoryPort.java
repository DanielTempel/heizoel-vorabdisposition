package heizoel.backend.confirmation.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponse;

import java.util.Optional;

public interface CustomerResponseRepositoryPort {

    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);
    Optional<CustomerResponse> findByConfirmationRequest(ConfirmationRequest confirmationRequest);

    CustomerResponse save(CustomerResponse customerResponse);
}
