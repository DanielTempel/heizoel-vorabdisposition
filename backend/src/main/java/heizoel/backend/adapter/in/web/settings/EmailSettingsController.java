package heizoel.backend.adapter.in.web.settings;

import heizoel.backend.adapter.in.web.security.CompanyContextResolver;
import heizoel.backend.adapter.in.web.settings.dto.EmailSettingsResponseDto;
import heizoel.backend.adapter.in.web.settings.dto.UpdateEmailSettingsRequestDto;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispo/settings/email")
@RequiredArgsConstructor
public class EmailSettingsController {

    private final CompanyContextResolver companyContextResolver;
    private final GetEmailSettingsUseCase getEmailSettingsUseCase;
    private final UpdateEmailSettingsUseCase updateEmailSettingsUseCase;
    private final TestEmailConnectionUseCase testEmailConnectionUseCase;
    private final SendTestEmailUseCase sendTestEmailUseCase;

    @GetMapping
    public EmailSettingsResponseDto getEmailSettings() {

        CompanyContext companyContext = companyContextResolver.resolve();
        GetEmailSettingsResult result = getEmailSettingsUseCase.getEmailSettings(companyContext);

        return EmailSettingsResponseDto.from(result);
    }

    @PutMapping
    public ResponseEntity<Void> updateEmailSettings(
            @Valid @RequestBody
            UpdateEmailSettingsRequestDto request
    ) {
        CompanyContext companyContext =
                companyContextResolver.resolve();

        updateEmailSettingsUseCase.updateEmailSettings(
                new UpdateEmailSettingsCommand(
                        companyContext,
                        request.smtpHost(),
                        request.smtpPort(),
                        request.securityMode(),
                        request.authenticationEnabled(),
                        request.username(),
                        request.password(),
                        request.fromAddress(),
                        request.fromName()
                )
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Void> testEmailConnection() {

        CompanyContext companyContext = companyContextResolver.resolve();
        testEmailConnectionUseCase.testEmailConnection(companyContext);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-message")
    public ResponseEntity<Void> sendTestEmail() {
        CompanyContext companyContext =
                companyContextResolver.resolve();

        sendTestEmailUseCase.sendTestEmail(
                companyContext
        );

        return ResponseEntity.noContent().build();
    }

}
