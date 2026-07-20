package heizoel.backend.adapter.out.persistence;

import heizoel.backend.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.CustomerResponse;
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
