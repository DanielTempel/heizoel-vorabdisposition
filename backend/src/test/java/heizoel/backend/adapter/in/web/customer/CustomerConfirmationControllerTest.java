package heizoel.backend.adapter.in.web.customer;

import heizoel.backend.application.port.in.confirmation.GetConfirmationPreviewUseCase;
import heizoel.backend.application.port.in.confirmation.SubmitCustomerResponseUseCase;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerConfirmationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerConfirmationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubmitCustomerResponseUseCase submitCustomerResponseUseCase;

    @MockitoBean
    GetConfirmationPreviewUseCase getConfirmationPreviewUseCase;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(
                Instant.parse("2026-08-05T10:00:00Z")
        );
    }

    @Test
    void returnsConflictWhenCustomerResponseAlreadyExists() throws Exception {
        doThrow(new CustomerResponseAlreadyExistsException(
                "A customer response already exists."
        )).when(submitCustomerResponseUseCase)
                .submitCustomerResponse(any());

        mockMvc.perform(post(
                        "/api/customer/confirmations/{token}/response",
                        "existing-response-token"
                )
                        .contentType("application/json")
                        .content("""
                            {
                              "responseType": "CONFIRM",
                              "customerComment": "Delivery is fine"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("CUSTOMER_RESPONSE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value(
                        "/api/customer/confirmations/existing-response-token/response"
                ))
                .andExpect(jsonPath("$.timestamp")
                        .value("2026-08-05T10:00:00Z"));
    }
}
