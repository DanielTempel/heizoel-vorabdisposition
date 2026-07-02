package heizoel.backend.confirmation.application.port.in;

public interface DispoConfirmationRequestUseCase {

    CreateConfirmationRequestResult createConfirmationRequest(
            CreateConfirmationRequestCommand command
    );
}
