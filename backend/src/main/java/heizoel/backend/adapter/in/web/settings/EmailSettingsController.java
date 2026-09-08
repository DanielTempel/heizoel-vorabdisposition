package heizoel.backend.adapter.in.web.settings;

import heizoel.backend.adapter.in.web.settings.dto.EmailSettingsResponseDto;
import heizoel.backend.adapter.in.web.settings.dto.UpdateEmailSettingsRequestDto;
import heizoel.backend.application.context.CompanyContext;
import heizoel.backend.application.port.in.settings.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/settings/email")
@RequiredArgsConstructor
@Slf4j
public class EmailSettingsController {

    private final GetEmailSettingsUseCase getEmailSettingsUseCase;
    private final UpdateEmailSettingsUseCase updateEmailSettingsUseCase;
    private final TestEmailConnectionUseCase testEmailConnectionUseCase;
    private final SendTestEmailUseCase sendTestEmailUseCase;

    @GetMapping
    public EmailSettingsResponseDto getEmailSettings(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        log.info(
                "Started getEmailSettings: companyId={}",
                companyContext.companyId()
        );
        GetEmailSettingsResult result = getEmailSettingsUseCase.getEmailSettings(companyContext);
        EmailSettingsResponseDto response = EmailSettingsResponseDto.from(result);

        log.info(
                "Completed getEmailSettings: companyId={}, configured={}, smtpHost={}, smtpPort={}, securityMode={}, authenticationEnabled={}, passwordConfigured={}, status=200",
                companyContext.companyId(),
                result.configured(),
                result.smtpHost(),
                result.smtpPort(),
                result.securityMode(),
                result.authenticationEnabled(),
                result.passwordConfigured()
        );
        return response;
    }

    @PutMapping
    public ResponseEntity<Void> updateEmailSettings(
            @AuthenticationPrincipal CompanyContext companyContext,
            @Valid @RequestBody UpdateEmailSettingsRequestDto request
    ) {
        log.info(
                "Started updateEmailSettings: companyId={}, smtpHost={}, smtpPort={}, securityMode={}, authenticationEnabled={}",
                companyContext.companyId(),
                request.smtpHost(),
                request.smtpPort(),
                request.securityMode(),
                request.authenticationEnabled()
        );
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

        log.info(
                "Completed updateEmailSettings: companyId={}, status=204",
                companyContext.companyId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Void> testEmailConnection(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        log.info(
                "Started testEmailConnection: companyId={}",
                companyContext.companyId()
        );
        testEmailConnectionUseCase.testEmailConnection(companyContext);

        log.info(
                "Completed testEmailConnection: companyId={}, status=204",
                companyContext.companyId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-message")
    public ResponseEntity<Void> sendTestEmail(
            @AuthenticationPrincipal CompanyContext companyContext
    ) {
        log.info(
                "Started sendTestEmail: companyId={}",
                companyContext.companyId()
        );
        sendTestEmailUseCase.sendTestEmail(companyContext);

        log.info(
                "Completed sendTestEmail: companyId={}, status=204",
                companyContext.companyId()
        );
        return ResponseEntity.noContent().build();
    }

}
