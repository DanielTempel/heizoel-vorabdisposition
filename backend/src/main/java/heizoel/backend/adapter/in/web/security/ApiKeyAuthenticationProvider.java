package heizoel.backend.adapter.in.web.security;

import heizoel.backend.adapter.out.persistence.CompanyRepository;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.company.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final CompanyRepository companyRepository;
    private final ApiKeyHasher apiKeyHasher;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String rawApiKey = (String) authentication.getCredentials();

        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BadCredentialsException("Invalid API key.");
        }

        String apiKeyHash = apiKeyHasher.hash(rawApiKey);

        Company company = companyRepository.findByApiKeyHash(apiKeyHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid API key."));

        CompanyContext companyContext = new CompanyContext(company.getId());

        return ApiKeyAuthenticationToken.authenticated(companyContext);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
