package heizoel.backend.dispo.application.interfaces;

import heizoel.backend.dispo.api.dto.request.DispoConfirmationRequestDto;
import heizoel.backend.dispo.application.model.DispoConfirmationCreationResult;

public interface DispoConfirmationService {

    DispoConfirmationCreationResult createConfirmationRequest(DispoConfirmationRequestDto request);
}
