package heizoel.backend.adapter.out.dispo;

import heizoel.backend.application.port.out.dispo.DispoStatusCallbackRequest;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.configuration.properties.ConfirmationProperties;
import heizoel.backend.configuration.DispoCallbackHttpConfig;
import heizoel.backend.shared.exception.DispoCallbackFailedException;
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

        service.sendStatusUpdate(new DispoStatusCallbackRequest(
                "http://dispo.example.test/api/confirmation-status-updates",
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

        assertThatThrownBy(() -> service.sendStatusUpdate(new DispoStatusCallbackRequest(
                "http://dispo.example.test/api/confirmation-status-updates",
                "A-CB-2",
                ConfirmationStatus.REJECTED,
                "Passt nicht."
        )))
                .isInstanceOf(DispoCallbackFailedException.class)
                .hasMessage("DISPO callback failed for externalOrderId=A-CB-2, status=REJECTED");

        server.verify();
    }
}
