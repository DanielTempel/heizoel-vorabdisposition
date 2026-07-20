package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponse;

import java.util.Optional;

public interface CustomerResponseRepositoryPort {

    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);
    Optional<CustomerResponse> findByConfirmationRequest(ConfirmationRequest confirmationRequest);

    CustomerResponse save(CustomerResponse customerResponse);
}
