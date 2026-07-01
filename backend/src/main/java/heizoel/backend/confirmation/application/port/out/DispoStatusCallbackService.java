package heizoel.backend.confirmation.application.port.out;

import heizoel.backend.confirmation.adapter.out.dispo.dto.DispoConfirmationStatusUpdateDto;

public interface DispoStatusCallbackService {

    void sendStatusUpdate(DispoConfirmationStatusUpdateDto statusUpdate);
}

