package heizoel.backend.adapter.in.web.settings;

import heizoel.backend.adapter.in.web.security.CompanyContextResolver;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.*;
import heizoel.backend.domain.company.SmtpSecurityMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailSettingsController.class)
class EmailSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyContextResolver companyContextResolver;

    @MockitoBean
    private GetEmailSettingsUseCase getEmailSettingsUseCase;

    @MockitoBean
    private UpdateEmailSettingsUseCase updateEmailSettingsUseCase;

    @MockitoBean
    private TestEmailConnectionUseCase testEmailConnectionUseCase;

    @MockitoBean
    private SendTestEmailUseCase sendTestEmailUseCase;

    @Test
    void returnsEmailSettingsWithoutPassword() throws Exception {
        CompanyContext companyContext =
                new CompanyContext(1L);

        when(companyContextResolver.resolve())
                .thenReturn(companyContext);

        when(getEmailSettingsUseCase.getEmailSettings(companyContext))
                .thenReturn(new GetEmailSettingsResult(
                        true,
                        "smtp.example.de",
                        587,
                        SmtpSecurityMode.STARTTLS,
                        true,
                        "dispo@example.de",
                        true,
                        "dispo@example.de",
                        "Example Heizöl",
                        Instant.parse("2026-08-05T20:03:00Z")
                ));

        mockMvc.perform(
                        get("/api/dispo/settings/email")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.smtpHost").value("smtp.example.de"))
                .andExpect(jsonPath("$.smtpPort").value(587))
                .andExpect(jsonPath("$.securityMode").value("STARTTLS"))
                .andExpect(jsonPath("$.authenticationEnabled").value(true))
                .andExpect(jsonPath("$.username").value("dispo@example.de"))
                .andExpect(jsonPath("$.passwordConfigured").value(true))
                .andExpect(jsonPath("$.fromAddress").value("dispo@example.de"))
                .andExpect(jsonPath("$.fromName").value("Example Heizöl"))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-05T20:03:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.encryptedPassword").doesNotExist());
    }

    @Test
    void returnsUnconfiguredState() throws Exception {
        CompanyContext companyContext =
                new CompanyContext(1L);

        when(companyContextResolver.resolve()).thenReturn(companyContext);

        when(getEmailSettingsUseCase.getEmailSettings(companyContext))
                .thenReturn(
                        GetEmailSettingsResult.notConfigured()
                );

        mockMvc.perform(get("/api/dispo/settings/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.smtpHost").isEmpty())
                .andExpect(jsonPath("$.passwordConfigured")
                        .value(false));
    }

    @Test
    void updatesEmailSettings() throws Exception {
        CompanyContext companyContext =
                new CompanyContext(1L);

        when(companyContextResolver.resolve())
                .thenReturn(companyContext);

        mockMvc.perform(
                        put("/api/dispo/settings/email")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "smtpHost": "smtp.example.de",
                                      "smtpPort": 587,
                                      "securityMode": "STARTTLS",
                                      "authenticationEnabled": true,
                                      "username": "smtp-user",
                                      "password": "smtp-secret",
                                      "fromAddress": "sender@example.de",
                                      "fromName": "Example Sender"
                                    }
                                    """)
                )
                .andExpect(status().isNoContent());

        verify(updateEmailSettingsUseCase)
                .updateEmailSettings(
                        new UpdateEmailSettingsCommand(
                                companyContext,
                                "smtp.example.de",
                                587,
                                SmtpSecurityMode.STARTTLS,
                                true,
                                "smtp-user",
                                "smtp-secret",
                                "sender@example.de",
                                "Example Sender"
                        )
                );
    }

    @Test
    void rejectsInvalidSmtpPort() throws Exception {
        when(companyContextResolver.resolve())
                .thenReturn(new CompanyContext(1L));

        mockMvc.perform(
                        put("/api/dispo/settings/email")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "smtpHost": "smtp.example.de",
                                      "smtpPort": 70000,
                                      "securityMode": "STARTTLS",
                                      "authenticationEnabled": false,
                                      "fromAddress": "sender@example.de",
                                      "fromName": "Example Sender"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));
    }

    @Test
    void testsEmailConnection() throws Exception {
        CompanyContext companyContext =
                new CompanyContext(1L);

        when(companyContextResolver.resolve())
                .thenReturn(companyContext);

        mockMvc.perform(
                        post(
                                "/api/dispo/settings/email/test-connection"
                        )
                )
                .andExpect(status().isNoContent());

        verify(testEmailConnectionUseCase)
                .testEmailConnection(companyContext);
    }

    @Test
    void sendsTestEmail() throws Exception {
        CompanyContext companyContext =
                new CompanyContext(1L);

        when(companyContextResolver.resolve())
                .thenReturn(companyContext);

        mockMvc.perform(
                        post(
                                "/api/dispo/settings/email/test-message"
                        )
                )
                .andExpect(status().isNoContent());

        verify(sendTestEmailUseCase)
                .sendTestEmail(companyContext);
    }

}