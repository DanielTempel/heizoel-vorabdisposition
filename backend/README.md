# Heizöl Vorabdisposition Backend

Backend prototype for the Heizöl delivery confirmation process.

The backend receives confirmation requests from a DISPO system, sends confirmation e-mails to customers, stores customer responses, updates confirmation statuses, and sends status updates back to DISPO via HTTP callback. Camunda handles the timeout case when the customer does not respond within the configured deadline.

---

## Tech Stack

* Java 25
* Spring Boot 3.5
* PostgreSQL 16
* Flyway
* Camunda Platform 7
* Spring Data JPA
* Spring Mail
* Thymeleaf mail templates
* Mailpit for local e-mail testing
* pgAdmin for database inspection
* DISPO Mock for local callback testing
* Swagger / OpenAPI
* Docker Compose for local infrastructure

---

## Project Structure

```text
backend/
├── src/                    # Main backend application
├── dispo-mock/             # Local DISPO mock service
├── docker-compose.yml      # Local infrastructure
└── README.md
```

The backend itself is started locally from IntelliJ or Maven during development.
Docker Compose starts the required infrastructure services.

---

## Local Development Architecture

```text
DISPO request / Postman / Swagger
        |
        | POST /api/dispo/confirmation-requests
        v
Backend :8080
        |
        | sends e-mail
        v
Mailpit :8025
        |
        | customer confirms/rejects via backend API
        v
Backend :8080
        |
        | HTTP callback
        v
DISPO Mock :8090
```

---

## Development Setup

For local development, start the infrastructure with Docker Compose:

* PostgreSQL
* Mailpit
* pgAdmin
* DISPO Mock

The backend is intentionally not started by Docker Compose during development. Run it locally with the `dev` Spring profile.

---

## Start Infrastructure

From the project root directory:

```bash
docker compose up -d
```

This starts:

| Service        | URL / Port              | Purpose                    |
| -------------- | ----------------------- | -------------------------- |
| PostgreSQL     | `localhost:5432`        | Backend database           |
| Mailpit SMTP   | `localhost:1025`        | Local SMTP server          |
| Mailpit Web UI | `http://localhost:8025` | Inspect sent e-mails       |
| pgAdmin        | `http://localhost:5050` | Inspect PostgreSQL         |
| DISPO Mock     | `http://localhost:8090` | Receives backend callbacks |

Check running containers:

```bash
docker compose ps
```

Stop infrastructure:

```bash
docker compose down
```

Stop and delete volumes:

```bash
docker compose down -v
```

Use `down -v` only if you want to delete local database data.

---

## Start Backend

### IntelliJ

Run `BackendApplication` with active Spring profile:

```text
dev
```

In IntelliJ:

```text
Run Configuration -> Active profiles -> dev
```

or add VM option:

```text
-Dspring.profiles.active=dev
```

### Terminal

From the backend directory:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend runs on:

```text
http://localhost:8080
```

Check the log for:

```text
The following 1 profile is active: "dev"
```

---

## Configuration Profiles

The application uses separate configuration files:

```text
application.yml
application-dev.yml
application-prod.yml
```

### `application.yml`

Contains common configuration shared by all profiles.

### `application-dev.yml`

Used for local development.

Typical local values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/heizoel_backend
    username: heizoel
    password: heizoel

  mail:
    host: localhost
    port: 1025

heizoel:
  confirmation:
    response-deadline: PT60S
    frontend-url: http://localhost:3000
    dispo-url: http://localhost:8090/api/dispo/confirmation-status-updates
```

### `application-prod.yml`

Used for production-like/demo deployment.

Production values should come from environment variables, not from hardcoded local values.

Example:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

heizoel:
  confirmation:
    response-deadline: ${CONFIRMATION_RESPONSE_DEADLINE:PT24H}
    frontend-url: ${FRONTEND_URL}
    dispo-url: ${DISPO_CALLBACK_URL}

  mail:
    from: ${MAIL_FROM:no-reply@heizoel.local}
```

---

## Important Configuration Values

| Property                                 | Meaning                                                 |
| ---------------------------------------- | ------------------------------------------------------- |
| `heizoel.confirmation.response-deadline` | Time until a confirmation request becomes `NO_RESPONSE` |
| `heizoel.confirmation.frontend-url`      | Base URL used in customer e-mail links                  |
| `heizoel.confirmation.dispo-url`         | HTTP callback URL for DISPO status updates              |
| `heizoel.mail.from`                      | Sender address for confirmation e-mails                 |
| `spring.mail.host`                       | SMTP host                                               |
| `spring.mail.port`                       | SMTP port                                               |

For local frontend development, the e-mail link points to:

```text
http://localhost:3000/confirmation/{token}
```

The frontend should extract `{token}` from the URL and call the backend customer API.

---

## Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Frontend developers can use Swagger UI to inspect and test the backend API.

---

## Mailpit

Mailpit catches outgoing e-mails locally.

Open:

```text
http://localhost:8025
```

When a DISPO confirmation request is created successfully, an e-mail appears in Mailpit.
The e-mail contains one customer confirmation link:

```text
http://localhost:3000/confirmation/{token}
```

Since the frontend may not be implemented yet, the link may lead to an empty page. For backend testing, copy the token and call the customer API directly.

---

## DISPO Mock

The DISPO Mock is a small local Spring Boot service that receives HTTP callbacks from the backend.

It runs on:

```text
http://localhost:8090
```

The backend sends callbacks to:

```text
POST http://localhost:8090/api/dispo/confirmation-status-updates
```

### Inspect received callbacks

```http
GET http://localhost:8090/api/dispo/confirmation-status-updates
```

Example response:

```json
[
  {
    "receivedAt": "2026-05-13T10:34:18.366Z",
    "externalOrderId": "A-2010",
    "confirmationStatus": "CONFIRMED",
    "customerComment": null
  }
]
```

### Clear received callbacks

```http
DELETE http://localhost:8090/api/dispo/confirmation-status-updates
```

Expected response:

```http
204 No Content
```

---

## pgAdmin

Open pgAdmin:

```text
http://localhost:5050
```

Login:

```text
Email:    admin@example.com
Password: admin
```

Create a new server connection.

### General

```text
Name: heizoel-postgres
```

### Connection

Because pgAdmin runs inside Docker, use the Docker service name as host:

```text
Host name/address: postgres
Port:              5432
Maintenance DB:    heizoel_backend
Username:          heizoel
Password:          heizoel
```

If connecting from a local desktop tool like DBeaver or IntelliJ Database Tool, use:

```text
Host: localhost
Port: 5432
Database: heizoel_backend
User: heizoel
Password: heizoel
```

---

## Main API Endpoints

## DISPO creates confirmation request

```http
POST /api/dispo/confirmation-requests
```

Example request:

```json
{
  "externalOrderId": "A-2001",
  "customerName": "Max Muller",
  "customerEmail": "daniel@example.com",
  "deliveryAddress": "Beispielstrasse 12, 97070 Wuerzburg",
  "product": "Heizoel",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00"
}
```

Example success response:

```json
{
  "externalOrderId": "A-2001",
  "confirmationStatus": "SENT"
}
```

Possible responses:

| Status            | Meaning                                            |
| ----------------- | -------------------------------------------------- |
| `201 Created`     | New confirmation request created and e-mail sent   |
| `200 OK`          | Duplicate unchanged request; no second e-mail sent |
| `400 Bad Request` | Validation error                                   |
| `502 Bad Gateway` | E-mail could not be sent                           |

---

## Customer gets confirmation preview

```http
GET /api/customer/confirmations/{token}
```

Example response:

```json
{
  "externalOrderId": "A-2001",
  "customerName": "Max Muller",
  "deliveryAddress": "Beispielstrasse 12, 97070 Wuerzburg",
  "product": "Heizoel",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00:00",
  "deliveryWindowEnd": "11:00:00"
}
```

---

## Customer confirms delivery window

```http
POST /api/customer/confirmations/{token}/confirm
```

Optional body:

```json
{
  "customerComment": "Bitte 30 Minuten vorher anrufen."
}
```

Success:

```http
204 No Content
```

---

## Customer rejects delivery window

```http
POST /api/customer/confirmations/{token}/reject
```

Optional body:

```json
{
  "customerComment": "Der Termin passt leider nicht."
}
```

Success:

```http
204 No Content
```

---

## DISPO Callback

When the customer confirms, rejects, or does not respond before the deadline, the backend sends an HTTP callback to DISPO.

Local development callback target:

```text
http://localhost:8090/api/dispo/confirmation-status-updates
```

Callback payload:

```json
{
  "externalOrderId": "A-2001",
  "confirmationStatus": "CONFIRMED",
  "customerComment": "Bitte 30 Minuten vorher anrufen."
}
```

Possible callback statuses:

```text
CONFIRMED
REJECTED
NO_RESPONSE
```

`SENT` is not sent as callback status. `SENT` is returned directly when DISPO creates the confirmation request.

If the callback fails, the Camunda job fails and is retried by the Camunda job executor according to the configured retry behavior.

---

## Confirmation Status Values

| Status        | Meaning                                                 |
| ------------- | ------------------------------------------------------- |
| `SENT`        | E-mail was sent and backend waits for customer response |
| `CONFIRMED`   | Customer confirmed the delivery window                  |
| `REJECTED`    | Customer rejected the delivery window                   |
| `NO_RESPONSE` | Customer did not respond before the deadline            |

---

## Error Response Format

All backend errors use the same response format:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Delivery window start must be before delivery window end.",
  "status": 400,
  "path": "/api/dispo/confirmation-requests",
  "timestamp": "2026-05-12T13:00:00Z"
}
```

Common error codes:

| Code                               | HTTP Status | Meaning                               |
| ---------------------------------- | ----------: | ------------------------------------- |
| `VALIDATION_ERROR`                 |         400 | Invalid request data                  |
| `EMAIL_SENDING_FAILED`             |         502 | Confirmation e-mail could not be sent |
| `CONFIRMATION_REQUEST_NOT_FOUND`   |         404 | Token does not exist                  |
| `CONFIRMATION_REQUEST_INACTIVE`    |         409 | Request is no longer active           |
| `CONFIRMATION_REQUEST_EXPIRED`     |         410 | Request has expired                   |
| `CUSTOMER_RESPONSE_ALREADY_EXISTS` |         409 | Customer already answered             |
| `NOT_FOUND`                        |         404 | Resource not found                    |
| `INTERNAL_ERROR`                   |         500 | Unexpected backend error              |

---

## Local End-to-End Test Flow

## 1. Start infrastructure

```bash
docker compose up -d
```

## 2. Start backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

or run `BackendApplication` in IntelliJ with profile `dev`.

## 3. Create confirmation request

```http
POST http://localhost:8080/api/dispo/confirmation-requests
```

Use the example DISPO request body.

Expected:

```http
201 Created
```

## 4. Open Mailpit

```text
http://localhost:8025
```

Open the received e-mail and copy the token from the confirmation link.

Example link:

```text
http://localhost:3000/confirmation/abc123
```

The token is:

```text
abc123
```

## 5. Test preview without frontend

```http
GET http://localhost:8080/api/customer/confirmations/{token}
```

## 6. Confirm without frontend

```http
POST http://localhost:8080/api/customer/confirmations/{token}/confirm
```

Expected:

```http
204 No Content
```

## 7. Check database

```sql
SELECT external_order_id, confirmation_status
FROM order_snapshot
WHERE external_order_id = 'A-2001';
```

Expected:

```text
CONFIRMED
```

## 8. Check DISPO callback

```http
GET http://localhost:8090/api/dispo/confirmation-status-updates
```

Expected: one callback with status:

```text
CONFIRMED
```

---

## Camunda Timeout

After a confirmation request is sent, the backend starts a Camunda process.
The process waits for:

```yaml
heizoel.confirmation.response-deadline
```

If the customer does not answer before the deadline:

```text
confirmation_status = NO_RESPONSE
confirmation_request.active = false
```

The backend then sends a callback to DISPO:

```json
{
  "externalOrderId": "A-2001",
  "confirmationStatus": "NO_RESPONSE",
  "customerComment": null
}
```

For normal development this should usually be short enough for manual testing, for example:

```yaml
heizoel:
  confirmation:
    response-deadline: PT60S
```

For production-like configuration, use:

```yaml
heizoel:
  confirmation:
    response-deadline: PT24H
```

---

## Useful SQL Queries

## Check order status

```sql
SELECT external_order_id, confirmation_status
FROM order_snapshot
ORDER BY id DESC;
```

## Check confirmation requests

```sql
SELECT
    os.external_order_id,
    cr.id,
    cr.active,
    cr.sent_at,
    cr.expires_at,
    cr.token
FROM confirmation_request cr
JOIN order_snapshot os ON os.id = cr.order_snapshot_id
ORDER BY cr.id DESC;
```

## Check customer responses

```sql
SELECT
    os.external_order_id,
    crs.response_type,
    crs.comment,
    crs.received_at
FROM customer_response crs
JOIN confirmation_request cr ON cr.id = crs.confirmation_request_id
JOIN order_snapshot os ON os.id = cr.order_snapshot_id
ORDER BY crs.id DESC;
```

## Check latest order with callback-relevant information

```sql
SELECT
    os.confirmation_status,
    os.external_order_id,
    os.customer_name,
    os.customer_email,
    os.delivery_address,
    os.product,
    os.quantity_liters,
    cr.id AS confirmation_request_id,
    cr.active,
    cr.sent_at,
    cr.expires_at,
    cr.token
FROM order_snapshot os
LEFT JOIN confirmation_request cr ON cr.order_snapshot_id = os.id
ORDER BY os.id DESC, cr.id DESC;
```

---

## Notes for Frontend Developers

The frontend customer page should use this route:

```text
/confirmation/{token}
```

Frontend flow:

1. Extract `{token}` from the URL.
2. Call:

```http
GET /api/customer/confirmations/{token}
```

3. Show the delivery information.
4. Let the customer confirm or reject.
5. Submit one of:

```http
POST /api/customer/confirmations/{token}/confirm
POST /api/customer/confirmations/{token}/reject
```

The request body is optional:

```json
{
  "customerComment": "Optional customer comment"
}
```

On success, backend returns:

```http
204 No Content
```

The frontend does not call DISPO directly. DISPO status updates are handled by the backend via HTTP callback.

---

## Current MVP Limitations

* Real DISPO is not connected in local development; DISPO Mock is used instead.
* DISPO callback is implemented as HTTP client, but local testing uses DISPO Mock.
* Callback retry relies on Camunda job retry behavior.
* Real SMTP provider is not configured yet; Mailpit is used locally.
* Token is stored as plain text for MVP.
* Authentication/authorization is not part of the MVP.
* Frontend is developed separately.
