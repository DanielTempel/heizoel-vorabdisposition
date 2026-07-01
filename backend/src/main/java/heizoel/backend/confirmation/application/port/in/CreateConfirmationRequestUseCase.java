package heizoel.backend.confirmation.application.port.in;

public interface CreateConfirmationRequestUseCase {

    CreateConfirmationRequestResult createConfirmationRequest(
            CreateConfirmationRequestCommand command
    );
}
