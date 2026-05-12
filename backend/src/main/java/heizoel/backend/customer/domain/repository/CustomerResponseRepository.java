package heizoel.backend.customer.domain.repository;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.customer.domain.entity.CustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerResponseRepository extends JpaRepository<CustomerResponse, Long> {

    boolean existsByConfirmationRequest(ConfirmationRequest confirmationRequest);
}
