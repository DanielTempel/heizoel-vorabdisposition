# Backend Architecture

## Purpose

The backend implements the digital confirmation process for planned Heizöl deliveries. It receives delivery data from DISPO, communicates with customers, persists the confirmation history, runs durable workflows, and exposes customer- and dispatcher-facing APIs.

This document explains stable responsibilities and boundaries. For exact endpoint schemas, workflow elements, database columns, or dependency versions, use the source files linked in [Sources of Truth](#sources-of-truth).

## Architectural Style

The backend follows a ports-and-adapters (hexagonal) architecture:

Runtime interaction:

```text
Inbound Adapter
      │
      ▼
Application ──► Outbound Port ──► Outbound Adapter
      │
      ▼
    Domain
```

Compile-time dependency direction:

```text
Inbound Adapter ──► Application ◄── Outbound Adapter
                         │
                         ▼
                       Domain
```

Outbound ports are defined by the application layer and implemented by outbound adapters. Runtime calls cross those ports toward adapters, while compile-time dependencies point toward application and domain responsibilities. Framework- and protocol-specific details stay in adapters.

## Main Areas

### Domain

`src/main/java/heizoel/backend/domain/` contains business entities, value objects, and invariants. The central concepts are:

- `Company`: tenant identity and company-specific integration context.
- `Order`: the current delivery and confirmation state for a company-specific external order.
- `ConfirmationRequest`: one customer confirmation attempt associated with an order.
- `CustomerResponse`: the customer's confirm or reject decision for a request.
- `DeliverySlot` and `Tour`: delivery scheduling values.
- `CompanyEmailSettings`: company-specific SMTP configuration.

Domain code owns business state transitions and invariants.

Domain behavior does not depend on HTTP, Camunda, SMTP, or concrete repository implementations. Domain entities are currently mapped with JPA annotations, while persistence queries and repository implementations remain isolated in outbound persistence adapters.

### Application

`src/main/java/heizoel/backend/application/` contains use cases and orchestration.

- `port/in/` defines operations used by web controllers and Camunda delegates.
- `port/out/` defines infrastructure capabilities needed by the application.
- `service/` coordinates repositories, domain objects, notifications, workflows, tracking, and settings.
- `context/CompanyContext` carries the resolved tenant identity into company-scoped use cases.

Application services operate on commands and application models. They do not consume web DTOs.

### Inbound Adapters

`src/main/java/heizoel/backend/adapter/in/` translates external input into application calls:

- `web/dispo/`: confirmation requests from DISPO.
- `web/customer/`: token-based customer preview, response, and tracking operations.
- `web/overview/`: dispatcher dashboard and resend operations.
- `web/settings/`: company-specific e-mail settings.
- `web/security/`: company-context resolution.
- `camunda/`: workflow delegates that invoke application use cases.

Controllers are responsible for validation, context resolution, mapping, and HTTP responses. Business decisions remain in the application or domain layers.

### Outbound Adapters

`src/main/java/heizoel/backend/adapter/out/` implements external and technical capabilities:

- `persistence/`: Spring Data JPA and QueryDSL repositories and queries.
- `camunda/`: workflow start and message correlation.
- `notification/`: e-mail, SMS, and WhatsApp delivery adapters.
- `dispo/`: status callbacks to the company-specific DISPO URL.
- `location/`: geocoding and driver tracking.
- `security/`: encryption of stored SMTP passwords.
- `token/`: opaque customer token generation.

## Main Interaction Areas

```text
DISPO ──► inbound REST adapter ──► Application
Customer ──► inbound REST adapter ──► Application
Dashboard ──► inbound REST adapter ──► Application

Application ──► persistence ports ──► PostgreSQL adapters
Application ──► workflow port ──► Camunda adapter
Application ──► notification ports ──► communication adapters
Application ──► DISPO port ──► DISPO adapter
```

Detailed confirmation lifecycle behavior belongs in [Confirmation Workflow](confirmation-workflow.md). HTTP behavior belongs in [API](api.md), and dispatcher-facing query and resend behavior belongs in [Dashboard](dashboard.md).

## Tenant Isolation

Tenant isolation is a backend invariant. `CompanyContext` represents the tenant identity at the application boundary.

Company-owned operations resolve a `CompanyContext` in an inbound adapter and pass it into the application layer. Persistence access for tenant-owned data must be scoped by company ID. Customer-facing token access is isolated from dispatcher and company authentication.

New company-owned queries must use the existing company context. Frontend filtering is not a security boundary. Authentication mechanisms and profile-specific company resolution are documented in [Configuration](configuration.md).

## Persistence

PostgreSQL is the persistent store. Flyway owns schema evolution through migrations under `src/main/resources/db/migration/`. Persistence adapters isolate repository implementations, persistence queries, and QueryDSL from the application layer. Domain entities are currently mapped as JPA entities.

Historical confirmation requests and customer responses are retained. Current-state projections select the applicable request without deleting history.

## Durable Workflows and Idempotency

Camunda owns durable notification delivery, response deadlines, supersession, and final DISPO callbacks. BPMN service tasks call narrow application use cases through inbound delegates.

Workflow work can execute more than once. Application services therefore lock relevant aggregates and guard state transitions such as sending an already-sent request or marking an already-failed delivery. Exact timers, retry configuration, message names, and process paths belong in the BPMN model and its integration tests.

## Sources of Truth

| Concern | Authoritative source |
| --- | --- |
| Dependency and Java versions | `pom.xml` |
| Domain states and invariants | `src/main/java/.../domain/` and domain tests |
| Application behavior | `src/main/java/.../application/` and application tests |
| REST contracts | controllers, DTOs, Swagger/OpenAPI, and MVC tests |
| Workflow details | `src/main/resources/processes/` and Camunda integration tests |
| Database schema | `src/main/resources/db/migration/` |
| Runtime configuration | `application*.yml` and configuration-property classes |

Repository-specific engineering rules are defined in [`../AGENTS.md`](../AGENTS.md).
