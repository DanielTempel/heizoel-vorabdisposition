# Heizöl Avisierungsservice

Backend service for digital confirmation of planned Heizöl delivery windows.

The service integrates with DISPO, manages customer confirmation requests and asynchronous workflows, and provides APIs for customer- and dispatcher-facing functionality.

## Responsibilities

- Receive planned delivery and confirmation data from DISPO.
- Manage confirmation requests and customer responses.
- Execute durable asynchronous delivery, timeout, and callback workflows with Camunda.
- Integrate customer communication channels.
- Provide dispatcher dashboard, tracking, and settings APIs.
- Isolate company-owned data and manage company-specific backend settings.

## Tech Stack

- Java and Spring Boot
- PostgreSQL and Flyway
- Camunda Platform 7
- Spring Data JPA and QueryDSL
- Spring Mail and Thymeleaf mail templates
- OpenAPI / Swagger
- JUnit and Testcontainers
- Docker Compose

See [`pom.xml`](pom.xml) for the authoritative dependency and Java versions.

## Project Structure

```text
backend/
├── src/main/java/heizoel/backend/
│   ├── adapter/          inbound and outbound adapters
│   ├── application/      use cases, ports, and orchestration
│   ├── configuration/    Spring configuration and properties
│   └── domain/           business entities, values, and invariants
├── src/main/resources/
│   ├── db/               Flyway migrations and development seed data
│   ├── processes/        Camunda BPMN processes
│   └── templates/        notification templates
├── src/test/             unit and integration tests
├── docs/                 detailed backend documentation
├── dispo-mock/           local DISPO callback and tracking mock
├── docker-compose.yml    local infrastructure
├── AGENTS.md             repository-specific agent rules
└── pom.xml               build and dependency configuration
```

The code follows a ports-and-adapters architecture. See [Architecture](docs/architecture.md) for responsibilities and dependency boundaries.

## Prerequisites

- JDK 17 or newer
- Docker with Docker Compose

The Maven Wrapper is included, so a separate Maven installation is not required.

## Local Development

Create a `.env` file in the backend directory before starting the local stack. Docker Compose reads this file automatically. At minimum, provide the encryption key:

```dotenv
SECRET_ENCRYPTION_MASTER_KEY=<Base64-encoded 32-byte key>
```

See [Configuration](docs/configuration.md#secret-encryption) for key generation and the optional Twilio variables used for SMS and WhatsApp delivery.

For the Dispo demo button, also set `DEV_API_KEY` in this `.env` to the existing
API key of the company to present. Compose passes it to the frontend's Vite
server at runtime; it does not create or change a company credential.

Build and start the complete local stack, including the backend and frontend:

```bash
docker compose up -d --build
docker compose ps
```

Compose starts the backend with the `dev` profile at `http://localhost:8080`.
Open `http://localhost:3000/dispo` for the presentation and use the dashboard
button after the backend has finished starting. A separate `npm run dev` is no
longer needed. Stop any existing local frontend first to free port 3000.
See the [frontend README](../frontend/README.md) for standalone frontend development.

## Local Services

| Service | URL / Port | Purpose |
| --- | --- | --- |
| Backend | `http://localhost:8080` | Spring Boot application, built and started by Compose |
| Frontend | `http://localhost:3000/dispo` | Dispo presentation and dashboard, served by Vite in Compose |
| PostgreSQL | `localhost:5432` | Application database |
| Mailpit SMTP | `localhost:1025` | Local SMTP endpoint |
| Mailpit Web UI | `http://localhost:8025` | Inspect outgoing local e-mail |
| pgAdmin | `http://localhost:5050` | Inspect PostgreSQL |
| DISPO Mock | `http://localhost:8090` | Local status callback and tracking target |

The Compose credentials and service definitions are authoritative in [`docker-compose.yml`](docker-compose.yml). The `dev` profile seeds company `1` with Mailpit e-mail settings and dashboard demo data through the Flyway development callback.

## Configuration

The application uses Spring profiles:

- `application.yaml` contains shared configuration and the optional `.env` import.
- `application-dev.yml` configures local development and development seed data.
- `application-prod.yml` configures production-oriented external values and quieter logging.

Do not copy configuration blocks from documentation into these files without checking the current configuration classes and YAML. See [Configuration](docs/configuration.md) for property groups, environment variables, secrets, and company-specific settings.

## API & Swagger

With the backend running locally:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Swagger and the controller/DTO code are authoritative for exact request and response schemas. See [API](docs/api.md) for the API areas and asynchronous response semantics.

## Running Tests

Run the test suite with the Maven Wrapper:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Integration tests use PostgreSQL Testcontainers and cover persistence, Spring MVC, and Camunda workflow behavior. Docker must be available for tests that start containers.

## Technical Documentation

- [Architecture](docs/architecture.md)
- [Confirmation Workflow](docs/confirmation-workflow.md)
- [API](docs/api.md)
- [Dashboard](docs/dashboard.md)
- [Configuration](docs/configuration.md)

For agent-specific repository rules, see [`AGENTS.md`](AGENTS.md).

## Prototype Scope / Limitations

- The local DISPO integration is represented by a mock.
- SMS and WhatsApp delivery use Twilio and require the corresponding account, sender, and content-template configuration.
- Development uses a fixed company context for company `1`; the production profile resolves companies from `X-API-Key`.
- Geocoding uses the configured external provider when enabled and therefore may require network access.
