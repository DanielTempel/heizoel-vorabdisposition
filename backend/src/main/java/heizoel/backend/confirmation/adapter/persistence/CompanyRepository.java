package heizoel.backend.confirmation.adapter.persistence;

import heizoel.backend.confirmation.application.port.out.persistence.CompanyRepositoryPort;
import heizoel.backend.confirmation.domain.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, CompanyRepositoryPort{

    @Override
    Optional<Company> findByApiKeyHash(String apiKeyHash);

}
