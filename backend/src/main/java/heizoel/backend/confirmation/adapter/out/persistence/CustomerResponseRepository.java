package heizoel.backend.confirmation.adapter.out.persistence;

import heizoel.backend.confirmation.application.port.out.persistence.CustomerResponseRepositoryPort;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerResponseRepository
        extends JpaRepository<CustomerResponse, Long>, CustomerResponseRepositoryPort {

    @Override
    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);

}
