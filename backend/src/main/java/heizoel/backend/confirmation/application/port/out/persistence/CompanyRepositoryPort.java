package heizoel.backend.confirmation.application.port.out.persistence;

import heizoel.backend.confirmation.domain.model.Company;

import java.util.Optional;

public interface CompanyRepositoryPort {

    Optional<Company> findById(Long id);

    Optional<Company> findByApiKeyHash(String apiKeyHash);
}
