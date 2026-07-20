package heizoel.backend.application.port.in.confirmation;

public interface CreateConfirmationRequestUseCase {

    CreateConfirmationRequestResult createConfirmationRequest(
            CreateConfirmationRequestCommand command
    );
}
