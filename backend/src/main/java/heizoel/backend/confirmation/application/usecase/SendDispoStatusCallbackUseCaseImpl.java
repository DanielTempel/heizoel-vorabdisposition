package heizoel.backend.confirmation.application.usecase;

import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackCommand;
import heizoel.backend.confirmation.application.port.in.dispo.SendDispoStatusCallbackUseCase;
import heizoel.backend.confirmation.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.confirmation.application.port.out.dispo.DispoStatusCallbackService;
import heizoel.backend.confirmation.application.port.out.persistence.OrderSnapshotRepositoryPort;
import heizoel.backend.domain.exception.OrderSnapshotNotFoundException;
import heizoel.backend.domain.model.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendDispoStatusCallbackUseCaseImpl implements SendDispoStatusCallbackUseCase {

    private final DispoStatusCallbackService dispoStatusCallbackService;
    private final OrderSnapshotRepositoryPort orderSnapshotRepository;

    @Override
    public void sendDispoStatusCallback(SendDispoStatusCallbackCommand command) {

        OrderSnapshot orderSnapshot = orderSnapshotRepository.findById(command.orderSnapshotId())
                .orElseThrow(() -> new OrderSnapshotNotFoundException(
                        "Order snapshot was not found."
                ));

        dispoStatusCallbackService.sendStatusUpdate(
                new DispoStatusCallbackRequest(
                        orderSnapshot.getCompany().getCallbackUrl(),
                        orderSnapshot.getExternalOrderId(),
                        command.confirmationStatus(),
                        command.customerComment()
                )
        );
    }
}
