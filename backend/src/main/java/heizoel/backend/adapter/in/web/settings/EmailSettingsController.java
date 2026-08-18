package heizoel.backend.adapter.in.web.settings;

import heizoel.backend.adapter.in.web.settings.dto.EmailSettingsResponseDto;
import heizoel.backend.adapter.in.web.settings.dto.UpdateEmailSettingsRequestDto;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispo/settings/email")
@RequiredArgsConstructor
public class EmailSettingsController {

    private final GetEmailSettingsUseCase getEmailSettingsUseCase;
    private final UpdateEmailSettingsUseCase updateEmailSettingsUseCase;
    private final TestEmailConnectionUseCase testEmailConnectionUseCase;
    private final SendTestEmailUseCase sendTestEmailUseCase;

    @GetMapping
    public EmailSettingsResponseDto getEmailSettings(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        GetEmailSettingsResult result = getEmailSettingsUseCase.getEmailSettings(companyContext);

        return EmailSettingsResponseDto.from(result);
    }

    @PutMapping
    public ResponseEntity<Void> updateEmailSettings(
            @AuthenticationPrincipal CompanyContext companyContext,
            @Valid @RequestBody UpdateEmailSettingsRequestDto request
    ) {
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
    public ResponseEntity<Void> testEmailConnection(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        testEmailConnectionUseCase.testEmailConnection(companyContext);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-message")
    public ResponseEntity<Void> sendTestEmail(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        sendTestEmailUseCase.sendTestEmail(companyContext);

        return ResponseEntity.noContent().build();
    }

}
