package heizoel.backend.confirmation.adapter.web.security;

import heizoel.backend.confirmation.application.model.CompanyContext;
import heizoel.backend.confirmation.application.port.out.persistence.CompanyRepositoryPort;
import heizoel.backend.domain.model.Company;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ApiKeyCompanyContextResolver implements CompanyContextResolver {


    private static final String API_KEY_HEADER = "X-API-Key";

    private final HttpServletRequest request;
    private final CompanyRepositoryPort companyRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Override
    public CompanyContext resolve() {
        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new MissingApiKeyException("Missing API key.");
        }

        String apiKeyHash = apiKeyHasher.hash(rawApiKey);

        Company company = companyRepository.findByApiKeyHash(apiKeyHash)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key."));

        return new CompanyContext(company.getId());
    }

}
