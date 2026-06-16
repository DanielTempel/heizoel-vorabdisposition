package heizoel.backend.dispo.application;

import heizoel.backend.dispo.api.dto.response.DispoConfirmationStatusUpdateDto;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.infrastructure.ConfirmationProperties;
import heizoel.backend.dispo.infrastructure.DispoCallbackHttpConfig;
import heizoel.backend.exceptions.dispo.DispoCallbackFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@RestClientTest(HttpDispoStatusCallbackService.class)
@Import({
        DispoCallbackHttpConfig.class,
        ConfirmationProperties.class
})
@TestPropertySource(properties = {
        "heizoel.confirmation.dispo-url=http://dispo.example.test/api/confirmation-status-updates"
})
class HttpDispoStatusCallbackServiceIntegrationTest {

    @Autowired
    HttpDispoStatusCallbackService service;

    @Autowired
    MockRestServiceServer server;

    @Test
    void sendStatusUpdate_postsStatusUpdateToConfiguredDispoUrl() {
        server.expect(requestTo("http://dispo.example.test/api/confirmation-status-updates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "externalOrderId": "A-CB-1",
                          "confirmationStatus": "CONFIRMED",
                          "customerComment": "Bitte 30 Minuten vorher anrufen."
                        }
                        """))
                .andRespond(withNoContent());

        service.sendStatusUpdate(new DispoConfirmationStatusUpdateDto(
                "A-CB-1",
                ConfirmationStatus.CONFIRMED,
                "Bitte 30 Minuten vorher anrufen."
        ));

        server.verify();
    }

    @Test
    void sendStatusUpdate_throwsDispoCallbackFailedExceptionWhenDispoReturnsServerError() {
        server.expect(requestTo("http://dispo.example.test/api/confirmation-status-updates"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.sendStatusUpdate(new DispoConfirmationStatusUpdateDto(
                "A-CB-2",
                ConfirmationStatus.REJECTED,
                "Passt nicht."
        )))
                .isInstanceOf(DispoCallbackFailedException.class)
                .hasMessage("DISPO callback failed for externalOrderId=A-CB-2, status=REJECTED");

        server.verify();
    }
}
