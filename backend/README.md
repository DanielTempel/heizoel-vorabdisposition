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

Start the infrastructure from the backend directory:

```bash
docker compose up -d
docker compose ps
```

Before starting the backend, provide `SECRET_ENCRYPTION_MASTER_KEY` as a Base64-encoded 32-byte key. See [Configuration](docs/configuration.md#secret-encryption) for the key requirements.

Start the backend with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend is then available at `http://localhost:8080`.

## Local Services

| Service | URL / Port | Purpose |
| --- | --- | --- |
| Backend | `http://localhost:8080` | Spring Boot application, started separately |
| PostgreSQL | `localhost:5432` | Application database |
| Mailpit SMTP | `localhost:1025` | Local SMTP endpoint |
| Mailpit Web UI | `http://localhost:8025` | Inspect outgoing local e-mail |
| pgAdmin | `http://localhost:5050` | Inspect PostgreSQL |
| DISPO Mock | `http://localhost:8090` | Local status callback and tracking target |

The Compose credentials and service definitions are authoritative in [`docker-compose.yml`](docker-compose.yml). The `dev` profile seeds company `1` with Mailpit e-mail settings and dashboard demo data through the Flyway development callback.

## Configuration

The application uses Spring profiles:

- `application.yaml` contains shared configuration.
- `application-dev.yml` configures local development and development seed data.
- `application-prod.yml` configures production-oriented external values and optional `.env` loading.

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

- Local DISPO and SMS integrations are represented by mocks.
- Docker Compose does not currently include a local WhatsApp provider mock.
- Development uses a fixed company context for company `1`; the production profile resolves companies from `X-API-Key`.
- Geocoding uses the configured external provider when enabled and therefore may require network access.
