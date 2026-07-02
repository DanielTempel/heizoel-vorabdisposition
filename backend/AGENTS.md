# AGENTS.md

do tests only if I ask you, 

## Project Context

Backend prototype for Heizöl delivery confirmation.
The backend receives confirmation requests from DISPO, sends a customer confirmation message, stores the customer response, handles timeout via Camunda, and sends the final status back to DISPO.

## Tech Stack

* Java 25
* Spring Boot 3.5
* PostgreSQL 16
* Flyway
* Camunda Platform 7
* Spring Data JPA
* Spring Mail / Mailpit
* REST clients with Spring `RestClient`
* JUnit 5, Testcontainers, Mockito, Awaitility

## Architecture Rules

* Keep controllers thin.
* Put business logic into application/domain services.
* Keep external systems behind interfaces.
* Do not expose JPA entities through REST APIs.
* Use DTO records for API contracts.
* Use Flyway for every database schema change.
* Keep `spring.jpa.hibernate.ddl-auto=validate`.
* `GET` endpoints must not change state.
* Customer confirm/reject actions must use `POST`.

## Main Flow

```text
DISPO -> Backend -> Notification channel -> Customer
Customer -> Backend -> Camunda callback workflow -> DISPO
```

The DISPO system is external. During local development it is represented by `dispo-mock`.

## Important Business Rules

Confirmation statuses:

```text
SENT
CONFIRMED
REJECTED
NO_RESPONSE
```

A request is a duplicate only if there is an active request for the same order and the relevant data is unchanged.

Relevant duplicate-check data includes:

* order snapshot data
* delivery date
* delivery time window
* communication channel

Inactive old requests must not block a new request.

Important case:

```text
SENT -> NO_RESPONSE -> dispatcher sends again
```

Expected behavior:

```text
new confirmation_request is created
old request stays inactive
order_snapshot.confirmation_status becomes SENT again
```

If the communication channel changes, for example from `EMAIL` to `SMS`, a new request must be created.

## Communication Channels

Currently supported:

```text
EMAIL
SMS
WHATSAPP
```

Channel-specific validation is required:

* `EMAIL` requires customer e-mail.
* `SMS` requires customer phone number.
* `WHATSAPP` requires customer phone number.

## Camunda Rules

Camunda is used for asynchronous process behavior:

* timeout handling
* DISPO callback retry behavior

Timeout process:

* waits for `heizoel.confirmation.response-deadline`
* checks whether the request is still active
* checks whether a customer response already exists
* sets status to `NO_RESPONSE` only if still unanswered

Timeout must not overwrite `CONFIRMED` or `REJECTED`.

If DISPO callback fails, the Camunda job may fail intentionally so Camunda can retry it.

## Configuration Rules

Use profile-based configuration:

```text
application.yml
application-dev.yml
application-prod.yml
```

`application.yml` should contain shared defaults.

`application-dev.yml` should contain local development values, for example localhost URLs, Mailpit, mock services, short timeout.

`application-prod.yml` should be production-like and use environment variables for external URLs and credentials.

During local development, run backend with:

```text
-Dspring.profiles.active=dev
```

## Local Development

Infrastructure and mocks are started with Docker Compose.

Backend is usually started locally from IntelliJ or Maven during active development.

Expected local services:

```text
Backend:     http://localhost:8080
Mailpit:     http://localhost:8025
pgAdmin:     http://localhost:5050
DISPO Mock:  http://localhost:8090
SMS Mock:    http://localhost:8091
Swagger:     http://localhost:8080/swagger-ui.html
```

## Testing Rules

Use integration tests for important business behavior.

Typical setup:

* `@SpringBootTest`
* `@AutoConfigureMockMvc`
* PostgreSQL Testcontainer
* `JdbcTemplate` for DB assertions
* `@MockitoBean` for external service interfaces
* Awaitility for Camunda async behavior

Important scenarios to keep covered:

* create confirmation request
* duplicate unchanged active request
* changed data creates new request
* changed channel creates new request
* customer confirms
* customer rejects
* no response timeout
* timeout does not overwrite customer response
* DISPO callback is triggered
* DISPO callback failure creates retryable Camunda job
* EMAIL and SMS channel behavior

## Current MVP Limitations

* Real DISPO is replaced by `dispo-mock`.
* Real SMS provider is replaced by `sms-mock`.
* Real WhatsApp integration is not implemented.
* Real SMTP provider is not configured.
* Token hashing may be added later.
* Authentication/authorization is outside current MVP scope.

## Do Not

* Do not put business logic into controllers.
* Do not mutate state from `GET` endpoints.
* Do not treat inactive requests as duplicates.
* Do not remove communication channel from duplicate detection.
* Do not change database schema without Flyway migration.
* Do not expose internal technical IDs in external API contracts unless explicitly required.
* Do not hardcode production credentials.
