package heizoel.backend.application.port.in.confirmation;

public interface ResendConfirmationRequestUseCase {

    ResendConfirmationRequestResult resend(
            ResendConfirmationRequestCommand command
    );
}