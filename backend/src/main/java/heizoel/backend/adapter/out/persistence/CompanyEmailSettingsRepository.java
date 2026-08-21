package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.company.CompanyEmailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyEmailSettingsRepository   extends JpaRepository<CompanyEmailSettings, Long> {

    Optional<CompanyEmailSettings> findByCompanyId(Long companyId);

}

