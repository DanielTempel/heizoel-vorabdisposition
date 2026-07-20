package heizoel.backend.adapter.in.web.security;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.Company;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
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
    private final CompanyRepository companyRepository;
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
