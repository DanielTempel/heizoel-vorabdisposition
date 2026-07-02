package heizoel.backend.confirmation.application.port.in.dispo;

public interface SendDispoStatusCallbackUseCase {

    void sendDispoStatusCallback(SendDispoStatusCallbackCommand command);
}
