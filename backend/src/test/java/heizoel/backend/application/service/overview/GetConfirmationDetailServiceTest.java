package heizoel.backend.application.service.overview;

import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.overview.GetConfirmationDetailQuery;
import heizoel.backend.application.port.out.persistence.ConfirmationDetailQueryPort;
import heizoel.backend.domain.ConfirmationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetConfirmationDetailServiceTest {

    private static final long COMPANY_ID = 7L;
    private static final String EXTERNAL_ORDER_ID = "ORDER-4711";

    @Mock
    ConfirmationDetailQueryPort confirmationDetailQueryPort;

    GetConfirmationDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetConfirmationDetailService(confirmationDetailQueryPort);
    }

    @Test
    void returnsDetailFromPort() {
        ConfirmationDetail detail = detail();
        when(confirmationDetailQueryPort.findDetail(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.of(detail));

        ConfirmationDetail result = service.getOrderDetail(query());

        assertThat(result).isSameAs(detail);
        verify(confirmationDetailQueryPort).findDetail(
                COMPANY_ID,
                EXTERNAL_ORDER_ID
        );
    }

    @Test
    void throwsWhenOrderIsNotFound() {
        when(confirmationDetailQueryPort.findDetail(COMPANY_ID, EXTERNAL_ORDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderDetail(query()))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order was not found.");

        verify(confirmationDetailQueryPort).findDetail(
                COMPANY_ID,
                EXTERNAL_ORDER_ID
        );
    }

    private GetConfirmationDetailQuery query() {
        return new GetConfirmationDetailQuery(
                new CompanyContext(COMPANY_ID),
                EXTERNAL_ORDER_ID
        );
    }

    private ConfirmationDetail detail() {
        return new ConfirmationDetail(
                new ConfirmationDetail.OrderDetail(
                        EXTERNAL_ORDER_ID,
                        "Customer",
                        "customer@example.test",
                        "+49123456789",
                        "Example Street 1",
                        "Heating oil",
                        2_500,
                        "2,500 EUR",
                        "A-17",
                        "WUE-DEMO 100",
                        ConfirmationStatus.SENT
                ),
                null,
                List.of()
        );
    }
}
