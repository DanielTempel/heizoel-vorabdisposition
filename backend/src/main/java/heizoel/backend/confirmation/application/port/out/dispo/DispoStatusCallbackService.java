package heizoel.backend.confirmation.application.port.out.dispo;

public interface DispoStatusCallbackService {

    void sendStatusUpdate(DispoStatusCallbackRequest request);
}
