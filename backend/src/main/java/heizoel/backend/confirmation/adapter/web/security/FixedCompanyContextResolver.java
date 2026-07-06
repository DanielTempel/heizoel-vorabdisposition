package heizoel.backend.confirmation.adapter.web.security;

import heizoel.backend.confirmation.application.model.CompanyContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class FixedCompanyContextResolver implements CompanyContextResolver {

    @Override
    public CompanyContext resolve() {
        return new CompanyContext(1L);
    }
}
