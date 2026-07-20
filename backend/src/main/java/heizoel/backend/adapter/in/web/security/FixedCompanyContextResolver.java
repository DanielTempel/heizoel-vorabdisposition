package heizoel.backend.adapter.in.web.security;

import heizoel.backend.application.context.CompanyContext;
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
