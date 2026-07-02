package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.model.ConfirmationRequestData;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;

public interface ConfirmationRequestPreparationService {

    ConfirmationRequestCreationResult prepareConfirmationRequest(
            OrderSnapshotData orderData,
            ConfirmationRequestData requestData
    );

}
