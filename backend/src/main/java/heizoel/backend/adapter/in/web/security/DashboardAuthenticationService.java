package heizoel.backend.adapter.in.web.security;

import heizoel.backend.application.context.CompanyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAuthenticationService {

    private final SecurityContextRepository securityContextRepository;

    public void authenticate(
            CompanyContext companyContext,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication =
                new PreAuthenticatedAuthenticationToken(
                        companyContext,
                        null,
                        List.of()
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );
    }
}