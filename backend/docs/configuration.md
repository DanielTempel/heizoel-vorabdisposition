# Backend Configuration

## Purpose

This document explains configuration responsibilities and property groups without duplicating complete YAML files. The active `application*.yml` files and `@ConfigurationProperties` classes are authoritative for exact defaults and available fields.

## Spring Profiles

| File | Responsibility |
| --- | --- |
| `src/main/resources/application.yaml` | Shared application, JPA/Flyway, Camunda, provider, geocoding, encryption, and logging configuration. |
| `src/main/resources/application-dev.yml` | Local PostgreSQL, development Flyway callback, local provider endpoints, and verbose SQL logging. |
| `src/main/resources/application-prod.yml` | Production-oriented datasource/provider values, optional `.env` import, and quieter logging. |

Start the local profile as described in the [README](../README.md#local-development). Standard Spring Boot precedence applies: command-line arguments, environment variables, and external configuration can override packaged YAML values.

The `prod` profile optionally imports `.env` through `spring.config.import`. The `dev` profile does not add that import; provide required secrets to the process environment or another standard Spring configuration source when running locally.

## Property Groups

### Application and Persistence

| Property group | Responsibility |
| --- | --- |
| `server.port` | HTTP port; shared configuration uses `8080`. |
| `spring.datasource.*` | PostgreSQL connection. |
| `spring.jpa.*` | Hibernate validation and SQL logging behavior. |
| `spring.flyway.*` | Schema migrations and profile-specific migration locations. |
| `camunda.bpm.*` | BPMN deployment configuration. |

Hibernate uses `ddl-auto: validate`; database schema changes belong in Flyway migrations, not Hibernate auto-update.

### Confirmation Integrations

`ConfirmationProperties` binds the `heizoel.confirmation` group:

| Property | Responsibility |
| --- | --- |
| `heizoel.confirmation.frontend-url` | Base URL used to create customer confirmation links and configure CORS. |
| `heizoel.confirmation.dispo-tracking-url` | Base URL used to request driver locations from DISPO. |
| `heizoel.confirmation.sms-provider-url` | SMS provider endpoint. |
| `heizoel.confirmation.whatsapp-provider-url` | WhatsApp provider endpoint. |

The final DISPO status callback URL is not a global confirmation property. It belongs to the current `Company` and is loaded from the database when a callback is sent.

### Geocoding

`LocationGeocodingProperties` binds `heizoel.location.geocoding`:

| Property | Responsibility |
| --- | --- |
| `enabled` | Enables delivery-address geocoding. |
| `provider` | Provider identifier. |
| `base-url` and `search-path` | Provider endpoint. |
| `user-agent` and `email` | Provider identification/contact values. |
| `country-code` and `accept-language` | Search localization. |
| `result-limit` | Maximum provider results requested. |
| `cache-ttl-minutes` | In-memory coordinate cache lifetime. |

The shared profile currently enables the configured external provider. Local work that must not use the network should override or disable geocoding explicitly.

## Environment Variables

`application-prod.yml` explicitly maps these environment variables:

| Variable | Target |
| --- | --- |
| `DB_URL` | JDBC datasource URL. |
| `DB_USERNAME` | Database username. |
| `DB_PASSWORD` | Database password. |
| `FRONTEND_URL` | Customer frontend base URL. |
| `SMS_PROVIDER_URL` | SMS provider endpoint. |
| `WHATSAPP_PROVIDER_URL` | WhatsApp provider endpoint. |

`SECRET_ENCRYPTION_MASTER_KEY` is referenced by shared configuration and is required in every profile.

Other Spring properties may also be overridden through normal Spring Boot external configuration and relaxed environment-variable binding. Check the active YAML and property classes before introducing a new deployment variable.

Global `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `DISPO_CALLBACK_URL` variables described by older documentation are not the current model. SMTP settings and callback URLs are company-specific.

## Secret Encryption

`SECRET_ENCRYPTION_MASTER_KEY` must be Base64 text that decodes to exactly 32 bytes. It is used as an AES-256-GCM key for stored company SMTP passwords.

For a temporary local PowerShell session, generate and export a key without writing it to the repository:

```powershell
$keyBytes = [byte[]]::new(32)
$keyGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
$keyGenerator.GetBytes($keyBytes)
$keyGenerator.Dispose()
$env:SECRET_ENCRYPTION_MASTER_KEY = [Convert]::ToBase64String($keyBytes)
```

For a shell with OpenSSL:

```bash
export SECRET_ENCRYPTION_MASTER_KEY="$(openssl rand -base64 32)"
```

Production must use a secret manager or equivalent protected injection mechanism. Do not commit the key or print it in logs. The production key must remain stable: changing it prevents decryption of SMTP passwords already stored with the old key.

Encrypted SMTP values are additionally bound to a company-specific encryption context. A password encrypted for one company cannot be reused as another company's stored value.

## Company-Specific Settings

### Company Resolution

Outside `prod`, `FixedCompanyContextResolver` selects company `1`. In `prod`, `ApiKeyCompanyContextResolver` reads `X-API-Key`, hashes it with SHA-256, and finds the corresponding company record.

Each `Company` owns:

- its external identity and API-key hash;
- its DISPO status callback URL; and
- its SMTP settings through `CompanyEmailSettings`.

### E-mail Settings

E-mail delivery is configured through `/api/dispo/settings/email`, not through global Spring Mail credentials.

Stored settings include SMTP host/port, transport security, authentication mode, optional username/encrypted password, and sender address/name. Read responses expose only whether a password is configured. Supported security modes are `STARTTLS`, `IMPLICIT_TLS`, and `NONE`.

The `dev` Flyway callback seeds company `1` with unauthenticated Mailpit settings (`localhost:1025`) when no settings exist. This keeps local e-mail delivery company-scoped while avoiding a copied `spring.mail` block.

## Local Infrastructure Configuration

`docker-compose.yml` is authoritative for local images, ports, volumes, and development credentials. The development datasource connects to its PostgreSQL service through `localhost:5432` when the backend runs on the host.

The `dev` profile adds both `classpath:db/migration` and `classpath:db/dev` to Flyway. The development callback maintains demo dashboard data and local SMTP settings; it must not be treated as production seed or schema history.

## Configuration Change Checklist

When adding or changing configuration:

1. update the relevant YAML and `@ConfigurationProperties` class;
2. keep secrets external to the repository;
3. update tests that bind or validate the property;
4. update this document when developer/operator responsibilities change; and
5. avoid copying full YAML blocks into the README.

## Related Documentation

- [Architecture](architecture.md)
- [Confirmation Workflow](confirmation-workflow.md)
- [API](api.md)
- [Dashboard](dashboard.md)
