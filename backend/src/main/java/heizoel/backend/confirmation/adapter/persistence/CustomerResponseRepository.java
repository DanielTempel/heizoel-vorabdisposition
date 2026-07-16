package heizoel.backend.confirmation.adapter.persistence;

import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.domain.model.ConfirmationRequest;
import heizoel.backend.domain.model.CustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerResponseRepository
        extends JpaRepository<CustomerResponse, Long>, CustomerResponseRepositoryPort {

    @Override
    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);

    @Override
    Optional<CustomerResponse> findByConfirmationRequest(
            ConfirmationRequest confirmationRequest
    );
}
