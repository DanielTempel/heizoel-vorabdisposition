package heizoel.backend.adapter.in.web.security;

import heizoel.backend.application.context.CompanyContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;


public final class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final CompanyContext principal;
    private final String credentials;

    private ApiKeyAuthenticationToken(
            CompanyContext principal,
            String credentials,
            boolean authenticated
    ) {
        super(List.of());

        this.principal = principal;
        this.credentials = credentials;

        super.setAuthenticated(authenticated);
    }

    public static ApiKeyAuthenticationToken unauthenticated(String rawApiKey) {
        return new ApiKeyAuthenticationToken(null, rawApiKey, false);
    }

    public static ApiKeyAuthenticationToken authenticated(CompanyContext companyContext) {
        return new ApiKeyAuthenticationToken(companyContext, null, true);
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public CompanyContext getPrincipal() {
        return principal;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot mark ApiKeyAuthenticationToken as authenticated directly."
            );
        }

        super.setAuthenticated(false);
    }

}
