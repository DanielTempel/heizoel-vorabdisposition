package heizoel.backend.application.port.in.confirmation;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.domain.CommunicationChannel;

public record ResendConfirmationRequestCommand(
        CompanyContext companyContext,
        String externalOrderId,
        CommunicationChannel communicationChannel,
        Integer responseDeadlineHours
) {
}