package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.adapter.in.web.dispo.dto.DispoConfirmationRequestDto;
import heizoel.backend.confirmation.application.model.DispoConfirmationCreationResult;

public interface DispoConfirmationService {

    DispoConfirmationCreationResult createConfirmationRequest(DispoConfirmationRequestDto request);
}

