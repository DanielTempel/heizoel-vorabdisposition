package heizoel.backend.application.port.in.workflow;

public interface SendConfirmationRequestUseCase {

    SendConfirmationRequestResult send(
            Long confirmationRequestId
    );
}
