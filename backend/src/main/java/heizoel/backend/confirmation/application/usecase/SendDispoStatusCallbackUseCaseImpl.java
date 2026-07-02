package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackCommand;
import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackUseCase;
import heizoel.backend.confirmation.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.confirmation.application.port.out.dispo.DispoStatusCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendDispoStatusCallbackUseCaseImpl implements SendDispoStatusCallbackUseCase {

    private final DispoStatusCallbackService dispoStatusCallbackService;

    @Override
    public void sendDispoStatusCallback(SendDispoStatusCallbackCommand command) {
        dispoStatusCallbackService.sendStatusUpdate(
                new DispoStatusCallbackRequest(
                        command.externalOrderId(),
                        command.confirmationStatus(),
                        command.customerComment()
                )
        );
    }
}
