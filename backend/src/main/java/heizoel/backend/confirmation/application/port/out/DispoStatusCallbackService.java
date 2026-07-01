package heizoel.backend.confirmation.application.port.out;

public interface DispoStatusCallbackService {

    void sendStatusUpdate(DispoStatusCallbackRequest request);
}