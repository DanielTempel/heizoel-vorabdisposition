package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.domain.model.enumeration.CustomerResponseType;

public record SubmitCustomerResponseCommand(
        String token,
        CustomerResponseType responseType,
        String customerComment
) {
}
