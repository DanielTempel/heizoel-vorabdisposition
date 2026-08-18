package heizoel.backend.application.port.in.workflow;

import java.time.Instant;

public record SendConfirmationRequestResult(
        Outcome outcome,
        Instant responseDeadlineAt
) {

    public enum Outcome {
        SENT,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static SendConfirmationRequestResult sent(
            Instant responseDeadlineAt
    ) {
        return new SendConfirmationRequestResult(
                Outcome.SENT,
                responseDeadlineAt
        );
    }

    public static SendConfirmationRequestResult retryableFailure() {
        return new SendConfirmationRequestResult(
                Outcome.RETRYABLE_FAILURE,
                null
        );
    }

    public static SendConfirmationRequestResult permanentFailure() {
        return new SendConfirmationRequestResult(
                Outcome.PERMANENT_FAILURE,
                null
        );
    }
}