package heizoel.backend.application.model.overview;

import heizoel.backend.application.model.overview.ConfirmationDetail.CustomerResponseDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.RequestDetail;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.NotificationDeliveryStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationDetailTest {

    @ParameterizedTest(name = "{0}, active={1}, response={2} -> {3}")
    @CsvSource(
            nullValues = "NONE",
            value = {
                    "PENDING, false, NONE,    PENDING",
                    "FAILED,  false, NONE,    FAILED",
                    "SENT,    true,  NONE,    SENT",
                    "SENT,    false, CONFIRM, CONFIRMED",
                    "SENT,    false, REJECT,  REJECTED",
                    "SENT,    false, NONE,    NO_RESPONSE"
            }
    )
    void status_mapsDeliveryAndResponseState(
            NotificationDeliveryStatus deliveryStatus,
            boolean active,
            CustomerResponseType responseType,
            String expectedStatus
    ) {
        RequestDetail request = requestDetail(
                deliveryStatus,
                active,
                responseType
        );

        assertThat(request.status()).isEqualTo(expectedStatus);
    }

    private RequestDetail requestDetail(
            NotificationDeliveryStatus deliveryStatus,
            boolean active,
            CustomerResponseType responseType
    ) {
        CustomerResponseDetail customerResponse =
                responseType == null
                        ? null
                        : new CustomerResponseDetail(
                                responseType,
                                null,
                                null
                        );

        return new RequestDetail(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                active,
                deliveryStatus,
                customerResponse
        );
    }
}
