package heizoel.backend.confirmation.application.port.in.confirmation;

public interface DispoConfirmationRequestUseCase {

    CreateConfirmationRequestResult createConfirmationRequest(
            CreateConfirmationRequestCommand command
    );
}
