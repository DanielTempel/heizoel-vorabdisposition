package heizoel.backend.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationStatusTest {

    @Test
    void fromRequestReturnsSentForActiveRequestWithoutResponse() {
        ConfirmationStatus status = ConfirmationStatus.fromRequest(
                true,
                null
        );

        assertThat(status).isEqualTo(ConfirmationStatus.SENT);
    }

    @Test
    void fromRequestReturnsNoResponseForInactiveRequestWithoutResponse() {
        ConfirmationStatus status = ConfirmationStatus.fromRequest(
                false,
                null
        );

        assertThat(status).isEqualTo(ConfirmationStatus.NO_RESPONSE);
    }

    @Test
    void fromRequestReturnsConfirmedForConfirmResponse() {
        ConfirmationStatus status = ConfirmationStatus.fromRequest(
                true,
                CustomerResponseType.CONFIRM
        );

        assertThat(status).isEqualTo(ConfirmationStatus.CONFIRMED);
    }

    @Test
    void fromRequestReturnsRejectedForRejectResponse() {
        ConfirmationStatus status = ConfirmationStatus.fromRequest(
                true,
                CustomerResponseType.REJECT
        );

        assertThat(status).isEqualTo(ConfirmationStatus.REJECTED);
    }
}
