package heizoel.backend.adapter.in.web.overview;

import heizoel.backend.adapter.in.web.security.ApiKeyAuthenticationToken;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.CustomerResponseDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.OrderDetail;
import heizoel.backend.application.model.overview.ConfirmationDetail.RequestDetail;
import heizoel.backend.application.port.in.confirmation.ResendConfirmationRequestUseCase;
import heizoel.backend.application.port.in.overview.GetConfirmationDetailQuery;
import heizoel.backend.application.port.in.overview.GetConfirmationDetailUseCase;
import heizoel.backend.application.port.in.overview.GetTourNumbersUseCase;
import heizoel.backend.application.port.in.overview.GetTourOverviewUseCase;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.domain.CustomerResponseType;
import heizoel.backend.domain.NotificationDeliveryStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    private static final CompanyContext COMPANY_CONTEXT = new CompanyContext(7L);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetTourOverviewUseCase getTourOverviewUseCase;

    @MockitoBean
    GetTourNumbersUseCase getTourNumbersUseCase;

    @MockitoBean
    GetConfirmationDetailUseCase getConfirmationDetailUseCase;

    @MockitoBean
    ResendConfirmationRequestUseCase resendConfirmationRequestUseCase;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                ApiKeyAuthenticationToken.authenticated(COMPANY_CONTEXT)
        );
        when(clock.instant()).thenReturn(Instant.parse("2026-08-05T10:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsConfirmationDetailJsonContract() throws Exception {
        when(getConfirmationDetailUseCase.getOrderDetail(any()))
                .thenReturn(detail());

        mockMvc.perform(get("/api/dispo/dashboard/orders/{externalOrderId}", "ORDER-4711"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.externalOrderId").value("ORDER-4711"))
                .andExpect(jsonPath("$.order.customerName").value("Erika Mustermann"))
                .andExpect(jsonPath("$.order.customerEmail").value("erika@example.test"))
                .andExpect(jsonPath("$.order.customerPhoneNumber").value("+49123456789"))
                .andExpect(jsonPath("$.order.deliveryAddress").value("Main Street 1"))
                .andExpect(jsonPath("$.order.product").value("Heating oil"))
                .andExpect(jsonPath("$.order.quantityLiters").value(2_500))
                .andExpect(jsonPath("$.order.priceDisplayText").value("2,500 EUR"))
                .andExpect(jsonPath("$.order.tourNumber").value("A-17"))
                .andExpect(jsonPath("$.order.vehicleLicensePlate").value("WUE-DEMO 100"))
                .andExpect(jsonPath("$.order.confirmationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.currentRequest.requestId").value(30))
                .andExpect(jsonPath("$.currentRequest.communicationChannel").value("EMAIL"))
                .andExpect(jsonPath("$.currentRequest.deliveryDate").value("2026-08-10"))
                .andExpect(jsonPath("$.currentRequest.deliveryWindowStart").value("08:00:00"))
                .andExpect(jsonPath("$.currentRequest.deliveryWindowEnd").value("10:00:00"))
                .andExpect(jsonPath("$.currentRequest.sentAt").value("2026-08-03T10:00:00Z"))
                .andExpect(jsonPath("$.currentRequest.expiresAt").value("2026-08-04T10:00:00Z"))
                .andExpect(jsonPath("$.currentRequest.responseDeadlineHours").value(24))
                .andExpect(jsonPath("$.currentRequest.active").value(false))
                .andExpect(jsonPath("$.currentRequest.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.currentRequest.customerResponse.responseType").value("CONFIRM"))
                .andExpect(jsonPath("$.currentRequest.customerResponse.comment").value("Delivery is fine"))
                .andExpect(jsonPath("$.currentRequest.customerResponse.receivedAt")
                        .value("2026-08-03T12:00:00Z"))
                .andExpect(jsonPath("$.previousRequests.length()").value(1))
                .andExpect(jsonPath("$.previousRequests[0].requestId").value(20))
                .andExpect(jsonPath("$.previousRequests[0].communicationChannel").value("SMS"))
                .andExpect(jsonPath("$.previousRequests[0].deliveryDate").value("2026-08-09"))
                .andExpect(jsonPath("$.previousRequests[0].deliveryWindowStart").value("09:00:00"))
                .andExpect(jsonPath("$.previousRequests[0].deliveryWindowEnd").value("11:00:00"))
                .andExpect(jsonPath("$.previousRequests[0].sentAt").value("2026-08-01T10:00:00Z"))
                .andExpect(jsonPath("$.previousRequests[0].expiresAt").value("2026-08-02T10:00:00Z"))
                .andExpect(jsonPath("$.previousRequests[0].responseDeadlineHours").value(24))
                .andExpect(jsonPath("$.previousRequests[0].active").value(false))
                .andExpect(jsonPath("$.previousRequests[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.previousRequests[0].customerResponse.responseType").value("REJECT"))
                .andExpect(jsonPath("$.previousRequests[0].customerResponse.comment")
                        .value("Please deliver another day"))
                .andExpect(jsonPath("$.previousRequests[0].customerResponse.receivedAt")
                        .value("2026-08-01T12:00:00Z"));

        ArgumentCaptor<GetConfirmationDetailQuery> captor =
                ArgumentCaptor.forClass(GetConfirmationDetailQuery.class);
        verify(getConfirmationDetailUseCase).getOrderDetail(captor.capture());
        assertThat(captor.getValue().companyContext()).isEqualTo(COMPANY_CONTEXT);
        assertThat(captor.getValue().externalOrderId()).isEqualTo("ORDER-4711");
    }

    @Test
    void returnsNotFoundErrorContract() throws Exception {
        when(getConfirmationDetailUseCase.getOrderDetail(any()))
                .thenThrow(new OrderNotFoundException("Order was not found."));

        mockMvc.perform(get("/api/dispo/dashboard/orders/{externalOrderId}", "MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_SNAPSHOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order was not found."))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/dispo/dashboard/orders/MISSING"));
    }

    private ConfirmationDetail detail() {
        return new ConfirmationDetail(
                new OrderDetail(
                        "ORDER-4711",
                        "Erika Mustermann",
                        "erika@example.test",
                        "+49123456789",
                        "Main Street 1",
                        "Heating oil",
                        2_500,
                        "2,500 EUR",
                        "A-17",
                        "WUE-DEMO 100",
                        ConfirmationStatus.CONFIRMED
                ),
                new RequestDetail(
                        30L,
                        CommunicationChannel.EMAIL,
                        LocalDate.of(2026, 8, 10),
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0),
                        Instant.parse("2026-08-03T10:00:00Z"),
                        Instant.parse("2026-08-04T10:00:00Z"),
                        24,
                        false,
                        NotificationDeliveryStatus.SENT,
                        new CustomerResponseDetail(
                                CustomerResponseType.CONFIRM,
                                "Delivery is fine",
                                Instant.parse("2026-08-03T12:00:00Z")
                        )
                ),
                List.of(new RequestDetail(
                        20L,
                        CommunicationChannel.SMS,
                        LocalDate.of(2026, 8, 9),
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0),
                        Instant.parse("2026-08-01T10:00:00Z"),
                        Instant.parse("2026-08-02T10:00:00Z"),
                        24,
                        false,
                        NotificationDeliveryStatus.SENT,
                        new CustomerResponseDetail(
                                CustomerResponseType.REJECT,
                                "Please deliver another day",
                                Instant.parse("2026-08-01T12:00:00Z")
                        )
                ))
        );
    }
}
