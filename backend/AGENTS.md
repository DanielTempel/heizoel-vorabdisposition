# AGENTS.md

## Purpose

This file defines repository-specific engineering rules for agents working on the Heizöl Vorabdisposition backend.

It is intentionally kept small and stable.

Do not use this file as a copy of the current business specification, API documentation, domain model, or Camunda workflow.

When implementation details are needed, inspect the corresponding source of truth in the repository.

---

## Project Context

The backend implements the digital confirmation process for planned Heizöl deliveries.

It integrates with an external DISPO system, communicates with customers, persists confirmation-related state, runs durable asynchronous workflows, and exposes dispatcher-facing backend functionality.

The backend follows a ports-and-adapters / hexagonal architecture.

---

## General Working Rules

Before changing behavior:

1. inspect the relevant production code;
2. inspect the relevant tests;
3. inspect related BPMN processes, migrations, configuration, or API contracts when applicable;
4. understand the existing invariant before modifying it.

Do not implement behavior based only on documentation if the current implementation says otherwise.

If documentation and implementation conflict, investigate the discrepancy before deciding which one is intended.

Prefer focused changes over unrelated refactoring.

Preserve existing behavior unless the task explicitly requires changing it.

---

## Sources of Truth

Do not duplicate frequently changing implementation details in this file.

Use the following repository artifacts as the primary sources of truth.

### Build and dependency versions

Use:

```text
pom.xml
````

Do not hardcode dependency or Java versions in this file.

### Domain states and business behavior

Use:

```text
src/main/java/.../domain/
src/test/java/.../domain/
```

Current enums, entities, value objects, domain methods, and their tests define the current domain model.

Do not maintain duplicate lists of statuses or state transitions here.

### Application behavior

Use:

```text
src/main/java/.../application/
src/test/java/.../application/
```

Application use cases and tests define orchestration and business workflows outside the domain model.

### REST API contracts

Use:

```text
controllers
request/response DTOs
OpenAPI / Swagger
controller and integration tests
```

Do not maintain endpoint inventories or DTO field lists in this file.

### Camunda workflow behavior

Use:

```text
src/main/resources/processes/
Camunda adapters
workflow integration tests
```

The BPMN model and its integration tests are authoritative for the current workflow structure.

Do not duplicate timers, retry counts, message names, or individual process transitions here.

### Database schema

Use:

```text
src/main/resources/db/migration/
```

Flyway migrations define the persisted schema.

### Configuration

Use:

```text
application*.yml
configuration classes
configuration properties
```

Do not assume configuration values without checking the current implementation.

### Detailed technical documentation

Technical developer documentation belongs under:

```text
backend/docs/
```

The repository-level `/docs` directory contains project and university documentation and is not automatically part of normal backend maintenance.

---

## Architecture Rules

Keep the existing ports-and-adapters boundaries.

### Domain

Domain code contains business concepts, invariants, and state transitions.

Domain code must not depend on:

* REST controllers;
* API DTOs;
* Camunda APIs;
* SMTP implementations;
* HTTP clients;
* concrete infrastructure adapters.

Prefer explicit domain methods for business state transitions instead of arbitrary field mutation from outside the domain object.

### Application

Application code contains use cases and orchestration.

Application services may coordinate:

* domain objects;
* repositories through ports;
* workflows;
* notifications;
* external integrations through ports.

Application code must not depend on inbound web DTOs.

### Inbound adapters

Controllers and Camunda delegates are adapters.

Keep controllers thin.

Controllers should normally:

1. receive and validate input;
2. resolve required context;
3. map input to a command or query;
4. invoke an application use case;
5. map the result to an external response.

Do not place business decisions in controllers.

### Outbound adapters

Infrastructure-specific implementations belong in outbound adapters.

External systems should remain behind application ports where appropriate.

Do not leak infrastructure-specific APIs into the domain layer.

---

## Multi-Tenancy

Tenant isolation is a hard backend invariant.

Company-owned data must always be scoped to the current company where applicable.

Do not rely on frontend filtering for tenant isolation.

When adding or changing queries involving tenant-owned data, explicitly verify that another company's data cannot be read or modified.

Use the existing company-context mechanism instead of introducing parallel tenant-resolution mechanisms.

---

## Persistence Rules

Use Flyway for every database schema change.

Do not use Hibernate automatic schema modification as a replacement for migrations.

Keep persistence entities internal to the backend.

Do not expose JPA entities directly through REST APIs.

Be deliberate about:

* transaction boundaries;
* lazy-loading;
* locking;
* concurrent workflow execution;
* historical data preservation.

Do not delete historical business records merely to simplify current-state handling unless the business requirement explicitly requires deletion.

---

## API Rules

External contracts use dedicated DTOs.

Do not expose internal persistence IDs unless the external contract requires them.

`GET` endpoints must not mutate business state.

Use HTTP status codes that reflect what has actually happened.

For asynchronous operations, do not return a response that falsely implies the asynchronous work has already completed.

When changing an external API contract, update its tests and relevant technical documentation.

---

## Time Handling

Business time must be testable.

Prefer the existing injected `Clock` where business decisions depend on the current time.

Avoid introducing uncontrolled calls to the system clock in business logic when an injectable clock can be used.

Keep time calculations centralized in the appropriate domain or application logic.

---

## Asynchronous and Workflow Code

Durable asynchronous operations must be designed for retries.

Assume that a workflow job may execute more than once.

Operations triggered by Camunda should therefore be idempotent where duplicate execution could cause incorrect business behavior or duplicate external effects.

Do not implement workflow retries using:

* blocking sleeps;
* ad-hoc application loops;
* unrelated schedulers.

Keep logically independent retry mechanisms independent.

When changing asynchronous behavior, explicitly consider:

* duplicate execution;
* stale workflow instances;
* concurrent requests;
* transaction boundaries;
* external side effects;
* failure after partial progress.

---

## Testing Rules

Behavior changes require tests.

Do not weaken or delete existing tests merely to make an implementation pass.

Prefer the lowest useful test level.

Use unit tests for isolated domain and application behavior.

Use integration tests when correctness depends on integration between components such as:

* PostgreSQL;
* Flyway;
* Spring MVC;
* QueryDSL;
* Camunda;
* tenant isolation;
* persistence locking;
* asynchronous behavior.

When fixing a bug, add a regression test when practical.

When changing a business invariant, search for existing tests that encode the previous invariant and update them deliberately.

Do not assume tests are green.

Run them.

---

## Documentation Rules

Documentation is part of the implementation when a change affects developer-visible behavior.

Update technical documentation when a change materially affects:

* architecture;
* external API contracts;
* domain concepts;
* important workflows;
* persistence;
* configuration;
* local development setup.

Do not duplicate every implementation detail in documentation.

Prefer documentation that explains:

* responsibilities;
* boundaries;
* important concepts;
* architectural decisions;
* externally relevant behavior.

Avoid documenting details that are easier and safer to discover directly from source code unless they are important for understanding or operating the system.

Do not automatically modify repository-level university/project deliverables under `/docs` during normal backend development.

---

## README Rules

`backend/README.md` is the backend entry point.

Keep it focused on:

* what the backend does;
* how to run it;
* local infrastructure;
* test execution;
* useful developer URLs;
* links to detailed technical documentation.

Do not turn the README into a second copy of the business logic or BPMN implementation.

---

## Change Discipline

Prefer simple solutions.

Do not introduce abstractions without a concrete reason.

Before creating a new:

* interface;
* service;
* factory;
* adapter;
* DTO layer;
* workflow;
* scheduler;

check whether the existing architecture already has the appropriate extension point.

Reuse existing business flows instead of creating parallel implementations for special cases.

Do not refactor unrelated areas as part of a focused feature or bug fix unless the existing structure directly blocks a correct implementation.

---

## Verification Before Completion

Before declaring a backend task complete:

1. inspect the final diff;
2. verify the intended behavior against the current source of truth;
3. run the relevant tests;
4. run broader tests when shared behavior was changed;
5. verify migrations when persistence changed;
6. verify tenant isolation when tenant-owned data was touched;
7. verify retry/idempotency behavior when asynchronous code was touched;
8. update relevant technical documentation;
9. check that no credentials or secrets were introduced;
10. check for accidental unrelated changes.

Do not claim that something works, is fixed, or is complete without verification evidence.

---

## Do Not

Do not:

* put business logic into controllers;
* expose JPA entities as external API contracts;
* mutate state through `GET` endpoints;
* bypass tenant isolation;
* change the database schema without Flyway;
* hardcode production credentials;
* duplicate mutable domain definitions in this file;
* duplicate BPMN implementation details in this file;
* invent current business behavior without inspecting code and tests;
* introduce parallel implementations when an existing use case or port should be reused;
* add ad-hoc schedulers to reproduce behavior already owned by Camunda;
* ignore retry and duplicate-execution behavior in asynchronous code;
* silently change external API contracts;
* knowingly leave technical documentation inconsistent with the resulting implementation;
* claim completion without running relevant verification.

