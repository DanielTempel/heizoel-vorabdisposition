package heizoel.backend.confirmation.adapter.web.security;

import heizoel.backend.confirmation.application.model.CompanyContext;

public interface ContextResolver {

    CompanyContext resolve();

}
