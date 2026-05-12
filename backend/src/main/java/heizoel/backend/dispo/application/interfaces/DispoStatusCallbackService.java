package heizoel.backend.dispo.application.interfaces;

import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;

public interface DispoStatusCallbackService {

    void sendStatusUpdate(DispoConfirmationStatusUpdateDto statusUpdate);
}
