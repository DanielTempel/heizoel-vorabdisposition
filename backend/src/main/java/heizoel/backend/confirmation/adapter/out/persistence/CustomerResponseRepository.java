package heizoel.backend.confirmation.adapter.out.persistence;

import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerResponseRepository extends JpaRepository<CustomerResponse, Long> {

    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);
}

