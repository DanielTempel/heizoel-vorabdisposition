package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.domain.CustomerResponseType;

public record SubmitCustomerResponseCommand(
        String token,
        CustomerResponseType responseType,
        String customerComment
) {
}
