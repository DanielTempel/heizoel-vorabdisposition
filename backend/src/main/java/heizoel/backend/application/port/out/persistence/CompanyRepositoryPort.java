package heizoel.backend.application.port.out.persistence;

import heizoel.backend.domain.Company;

import java.util.Optional;

public interface CompanyRepositoryPort {

    Optional<Company> findById(Long id);

    Optional<Company> findByApiKeyHash(String apiKeyHash);
}
