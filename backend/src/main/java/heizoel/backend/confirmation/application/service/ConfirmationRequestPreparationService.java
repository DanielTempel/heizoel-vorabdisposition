package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.domain.model.Company;

public interface ConfirmationRequestPreparationService {

    ConfirmationRequestCreationResult prepareConfirmationRequest(
            Company company,
            CreateConfirmationRequestCommand command
    );

}
