package heizoel.backend.confirmation.application.port.in;

public interface SendDispoStatusCallbackUseCase {

    void sendDispoStatusCallback(SendDispoStatusCallbackCommand command);
}
