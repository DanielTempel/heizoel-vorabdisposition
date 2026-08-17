package heizoel.backend.domain.company;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "company_email_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_email_settings_company_id",
                        columnNames = "company_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyEmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "smtp_host", nullable = false, length = 255)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_mode", nullable = false, length = 30)
    private SmtpSecurityMode securityMode;

    @Column(name = "authentication_enabled", nullable = false)
    private boolean authenticationEnabled;

    @Column(name = "smtp_username", length = 320)
    private String username;

    @Column(name = "smtp_password_encrypted", length = 2000)
    private String encryptedPassword;

    @Column(name = "from_address", nullable = false, length = 320)
    private String fromAddress;

    @Column(name = "from_name", nullable = false, length = 200)
    private String fromName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CompanyEmailSettings create(
            Company company,
            String smtpHost,
            Integer smtpPort,
            SmtpSecurityMode securityMode,
            boolean authenticationEnabled,
            String username,
            String encryptedPassword,
            String fromAddress,
            String fromName
    ) {
        CompanyEmailSettings settings = new CompanyEmailSettings();

        settings.company = company;
        settings.smtpHost = smtpHost;
        settings.smtpPort = smtpPort;
        settings.securityMode = securityMode;
        settings.fromAddress = fromAddress;
        settings.fromName = fromName;

        settings.applyAuthentication(
                authenticationEnabled,
                username,
                encryptedPassword
        );

        return settings;
    }

    public void update(
            String smtpHost,
            Integer smtpPort,
            SmtpSecurityMode securityMode,
            boolean authenticationEnabled,
            String username,
            String encryptedPassword,
            String fromAddress,
            String fromName
    ) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.securityMode = securityMode;
        this.fromAddress = fromAddress;
        this.fromName = fromName;

        applyAuthentication(
                authenticationEnabled,
                username,
                encryptedPassword
        );
    }

    public boolean hasConfiguredPassword() {
        return encryptedPassword != null && !encryptedPassword.isBlank();
    }

    private void applyAuthentication(
            boolean authenticationEnabled,
            String username,
            String encryptedPassword
    ) {
        this.authenticationEnabled = authenticationEnabled;

        if (authenticationEnabled) {
            this.username = username;
            this.encryptedPassword = encryptedPassword;
        } else {
            this.username = null;
            this.encryptedPassword = null;
        }
    }

    @PrePersist
    @PreUpdate
    private void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
