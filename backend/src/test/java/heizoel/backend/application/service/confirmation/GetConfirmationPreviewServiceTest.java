package heizoel.backend.application.service.confirmation;

import heizoel.backend.adapter.out.persistence.ConfirmationRequestRepository;
import heizoel.backend.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewResult;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponse;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.DeliverySlot;
import heizoel.backend.domain.Order;
import heizoel.backend.domain.Tour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetConfirmationPreviewServiceTest {

    private static final String TOKEN = "preview-token";
    private static final Instant SENT_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Mock
    ConfirmationRequestRepository confirmationRequestRepository;

    @Mock
    CustomerResponseRepository customerResponseRepository;

    GetConfirmationPreviewService service;

    @BeforeEach
    void setUp() {
        service = new GetConfirmationPreviewService(
                confirmationRequestRepository,
                customerResponseRepository
        );
    }

    @Test
    void returnsRejectedForInactiveHistoricalRequestWithRejectResponse() {
        Order order = order(ConfirmationStatus.CONFIRMED);
        ConfirmationRequest request = request(order, false);
        CustomerResponse response = CustomerResponse.create(
                request,
                CustomerResponseType.REJECT,
                "Please deliver another day",
                Instant.parse("2026-08-01T12:00:00Z")
        );
        mockRepositories(request, Optional.of(response));

        GetConfirmationPreviewResult result = service.getConfirmationPreview(TOKEN);

        assertThat(order.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.REJECTED);
        verify(customerResponseRepository).findByConfirmationRequest(request);
    }

    @Test
    void returnsNoResponseForInactiveRequestWithoutCustomerResponse() {
        ConfirmationRequest request = request(
                order(ConfirmationStatus.CONFIRMED),
                false
        );
        mockRepositories(request, Optional.empty());

        GetConfirmationPreviewResult result = service.getConfirmationPreview(TOKEN);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.NO_RESPONSE);
        verify(customerResponseRepository).findByConfirmationRequest(request);
    }

    @Test
    void returnsSentForActiveRequestWithoutCustomerResponse() {
        ConfirmationRequest request = request(
                order(ConfirmationStatus.CONFIRMED),
                true
        );
        mockRepositories(request, Optional.empty());

        GetConfirmationPreviewResult result = service.getConfirmationPreview(TOKEN);

        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
        verify(customerResponseRepository).findByConfirmationRequest(request);
    }

    private void mockRepositories(
            ConfirmationRequest request,
            Optional<CustomerResponse> response
    ) {
        when(confirmationRequestRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(request));
        when(customerResponseRepository.findByConfirmationRequest(request))
                .thenReturn(response);
    }

    private Order order(ConfirmationStatus status) {
        Order order = Order.create(
                Company.create(
                        "Company",
                        "api-key-hash",
                        "http://localhost/callback"
                ),
                "ORDER-4711",
                Tour.of("A-17", "WUE-DEMO 100"),
                "Erika Mustermann",
                "erika@example.test",
                "+49123456789",
                "Main Street 1",
                "Heating oil",
                2_500,
                "2,500 EUR"
        );

        if (status == ConfirmationStatus.CONFIRMED) {
            order.markConfirmed();
        }
        return order;
    }

    private ConfirmationRequest request(
            Order order,
            boolean active
    ) {
        ConfirmationRequest request = ConfirmationRequest.create(
                order,
                TOKEN,
                CommunicationChannel.EMAIL,
                DeliverySlot.of(
                        LocalDate.of(2026, 8, 10),
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0)
                ),
                SENT_AT,
                24
        );
        if (!active) {
            request.markInactive();
        }
        return request;
    }
}
