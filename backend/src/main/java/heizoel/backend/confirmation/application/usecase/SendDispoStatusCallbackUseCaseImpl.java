package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.SendDispoStatusCallbackCommand;
import heizoel.backend.confirmation.application.port.in.SendDispoStatusCallbackUseCase;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackRequest;
import heizoel.backend.confirmation.application.port.out.DispoStatusCallbackService;
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
