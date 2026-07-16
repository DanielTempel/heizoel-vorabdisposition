package heizoel.backend.confirmation.application.port.in.customer;

import heizoel.backend.domain.model.enumeration.CustomerResponseType;

public record SubmitCustomerResponseCommand(
        String token,
        CustomerResponseType responseType,
        String customerComment
) {
}
