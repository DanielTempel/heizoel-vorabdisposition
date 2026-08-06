CREATE TABLE company_email_settings
(
    id                      BIGSERIAL PRIMARY KEY,
    company_id              BIGINT       NOT NULL,
    smtp_host               VARCHAR(255) NOT NULL,
    smtp_port               INTEGER      NOT NULL,
    security_mode           VARCHAR(30)  NOT NULL,
    authentication_enabled  BOOLEAN      NOT NULL,
    smtp_username           VARCHAR(320),
    smtp_password_encrypted VARCHAR(2000),
    from_address            VARCHAR(320) NOT NULL,
    from_name               VARCHAR(200) NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_company_email_settings_company_id
        UNIQUE (company_id),

    CONSTRAINT fk_company_email_settings_company
        FOREIGN KEY (company_id)
            REFERENCES company (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_company_email_settings_smtp_port
        CHECK (smtp_port BETWEEN 1 AND 65535),

    CONSTRAINT ck_company_email_settings_security_mode
        CHECK (
            security_mode IN (
                              'STARTTLS',
                              'IMPLICIT_TLS',
                              'NONE'
                )
            ),

    CONSTRAINT ck_company_email_settings_authentication
        CHECK (
            (
                authentication_enabled = FALSE
                    AND smtp_username IS NULL
                    AND smtp_password_encrypted IS NULL
                )
                OR
            (
                authentication_enabled = TRUE
                    AND smtp_username IS NOT NULL
                    AND smtp_password_encrypted IS NOT NULL
                )
            )
);