# Heizöl Vorabdisposition Backend

Backend prototype for the Heizöl delivery confirmation process.
The backend receives confirmation requests from a DISPO system, sends confirmation links to customers via e-mail or SMS, stores customer responses, updates the confirmation status and sends status updates back to DISPO. Camunda workflows are used for customer response timeouts and retryable DISPO callbacks.

---

## Tech Stack

* Java 17
* Spring Boot 3.5
* PostgreSQL 16
* Flyway
* Camunda Platform 7
* Spring Data JPA
* Spring Mail
* Thymeleaf mail templates
* Mailpit for local e-mail testing
* DISPO Mock for local callback testing
* SMS Mock for local SMS testing
* pgAdmin for database inspection
* Swagger / OpenAPI
* Docker Compose for local infrastructure

---

## Project Structure

The backend is developed as a Spring Boot application.

Local supporting services are started with Docker Compose:

* PostgreSQL
* Mailpit
* pgAdmin
* DISPO Mock
* SMS Mock

The backend itself is normally started locally from IntelliJ or Maven during development.

---

## Development Setup

### Start Infrastructure

From the project root directory:

```bash
docker compose up -d
```

This starts the local infrastructure services.

| Service        | URL / Port              |
| -------------- | ----------------------- |
| PostgreSQL     | `localhost:5432`        |
| Mailpit SMTP   | `localhost:1025`        |
| Mailpit Web UI | `http://localhost:8025` |
| pgAdmin        | `http://localhost:5050` |
| DISPO Mock     | `http://localhost:8090` |
| SMS Mock       | `http://localhost:8091` |

Check running containers:

```bash
docker compose ps
```

## Configuration Profiles

The backend uses Spring profiles.

### `application.yml`

Contains common configuration shared by all profiles.

Typical shared values:

```yaml
spring:
  application:
    name: heizoel-backend

server:
  port: 8080

camunda:
  bpm:
    auto-deployment-enabled: true
    deployment-resource-pattern:
      - classpath*:processes/*.bpmn
```

### `application-dev.yml`

Used for local development.

Typical local values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/heizoel_backend
    username: heizoel
    password: heizoel
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  mail:
    host: localhost
    port: 1025
    username:
    password:
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
          connectiontimeout: 5000
          timeout: 3000
          writetimeout: 5000

heizoel:
  confirmation:
    frontend-url: http://localhost:3000
    dispo-url: http://localhost:8090/api/dispo/confirmation-status-updates
    dispo-tracking-url: http://localhost:8090/api/dispo/confirmation-status-updates/tracking/orders
    sms-provider-url: http://localhost:8091/api/sms/messages

  mail:
    from: no-reply@heizoel.local

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace
```

### `application-prod.yml`

The production profile uses environment variables for database and SMTP credentials as well as external frontend and DISPO callback URLs. No production credentials are stored in the repository.

Important environment variables include:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
FRONTEND_URL
DISPO_CALLBACK_URL
```

The DISPO tracking URL and SMS provider URL currently use shared defaults unless explicitly overridden in deployment configuration.

---

## Important Configuration Values

| Property / Field                    | Meaning                                                       |
| ----------------------------------- | ------------------------------------------------------------- |
| `heizoel.confirmation.frontend-url`       | Base URL used for customer confirmation links                 |
| `heizoel.confirmation.dispo-url`          | DISPO callback endpoint                                       |
| `heizoel.confirmation.dispo-tracking-url` | DISPO driver-location endpoint base URL                       |
| `heizoel.confirmation.sms-provider-url`   | SMS provider or SMS mock endpoint                             |
| `heizoel.location.geocoding.enabled`      | Enables or disables address geocoding                         |
| `heizoel.location.geocoding.base-url`     | Geocoding provider base URL                                   |
| `heizoel.location.geocoding.cache-ttl-minutes` | Geocoding result cache duration                           |
| `spring.mail.host`                        | SMTP host                                                     |
| `spring.mail.port`                        | SMTP port                                                     |
| `responseDeadlineHours`                   | DISPO request field defining how long the customer may answer |

Important: the customer response deadline is not a global backend timeout anymore. DISPO provides the requested deadline per request using `responseDeadlineHours`. The backend stores the requested hours in `confirmation_request.response_deadline_hours` and calculates the effective `expires_at`. If the requested deadline would be after the beginning of the delivery window, `expires_at` is capped at the delivery-window start. A delivery window that has already started is rejected with `400 Bad Request`. Camunda receives the effective absolute expiration timestamp and schedules the timeout job for that exact moment.

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

When a DISPO confirmation request is created with `communicationChannel = EMAIL`, an e-mail appears in Mailpit.

The e-mail contains a customer confirmation link:

```text
http://localhost:3000/confirmation/{token}
```

The frontend should extract `{token}` from the URL and call the backend customer API.

---

## SMS Mock

The SMS Mock is used for local SMS testing.

Open:

```text
http://localhost:8091
```

When a DISPO confirmation request is created with `communicationChannel = SMS`, the backend sends the customer confirmation link to the SMS Mock.

The SMS contains the same type of customer link:

```text
http://localhost:3000/confirmation/{token}
```

---

## DISPO Mock

The DISPO Mock receives callback status updates from the backend.

Callback endpoint:

```http
POST http://localhost:8090/api/dispo/confirmation-status-updates
```

The backend calls this endpoint when the customer confirms, rejects, or does not respond before the deadline.

View received callbacks:

```http
GET http://localhost:8090/api/dispo/confirmation-status-updates
```

Clear received callbacks:

```http
DELETE http://localhost:8090/api/dispo/confirmation-status-updates
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

## DISPO Creates Confirmation Request

```http
POST /api/dispo/confirmation-requests
```

### EMAIL Request Example

```json
{
  "externalOrderId": "A-2001",
  "customerName": "Max Muller",
  "communicationChannel": "EMAIL",
  "customerEmail": "daniel@example.com",
  "customerPhoneNumber": null,
  "deliveryAddress": "Beispielstrasse 12, 97070 Wuerzburg",
  "product": "Heizoel",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00",
  "responseDeadlineHours": 24,
  "priceDisplayText": "112,50 EUR / 100 Liter"
}
```

### SMS Request Example

```json
{
  "externalOrderId": "A-2002",
  "customerName": "Max Muller",
  "communicationChannel": "SMS",
  "customerEmail": null,
  "customerPhoneNumber": "+491701234567",
  "deliveryAddress": "Beispielstrasse 12, 97070 Wuerzburg",
  "product": "Heizoel",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00",
  "responseDeadlineHours": 24,
  "priceDisplayText": "112,50 EUR / 100 Liter"
}
```

### Success Response

```json
{
  "externalOrderId": "A-2001",
  "confirmationStatus": "SENT"
}
```

### Possible Responses

| Status            | Meaning                                             |
| ----------------- | --------------------------------------------------- |
| `201 Created`     | New confirmation request created and message sent   |
| `200 OK`          | Duplicate unchanged request; no second message sent |
| `400 Bad Request` | Validation error                                    |
| `422 Unprocessable Entity` | Required e-mail address or phone number is missing |
| `502 Bad Gateway` | E-mail/SMS sending failed                           |

---

## Customer Gets Confirmation Preview

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
  "deliveryWindowEnd": "11:00:00",
  "priceDisplayText": "112,50 EUR / 100 Liter",
  "confirmationStatus": "SENT"
}
```

The `confirmationStatus` allows the frontend to show whether the request is still open, already confirmed, rejected, or marked as no-response.

---

## Customer Gets Tracking Information

Tracking information is available on the delivery date.

```http
GET /api/customer/confirmations/{token}/tracking-info
```

Example response:

```json
{
  "trackingAvailable": true,
  "targetLocationX": 9.9372,
  "targetLocationY": 49.7935
}
```

`targetLocationX` represents longitude and `targetLocationY` represents latitude. The delivery address is resolved through the configured geocoding provider and cached for the configured TTL.

## Customer Gets Driver Location

```http
GET /api/customer/confirmations/{token}/driver-location
```

Example response:

```json
{
  "locationX": 9.8820,
  "locationY": 49.8166
}
```

The backend requests the current driver location from the configured DISPO tracking endpoint. The endpoint returns `404 Not Found` when tracking is not available for the delivery date or no driver location is available.

---

## Customer Confirms Delivery Window

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

## Customer Rejects Delivery Window

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

The backend sends a callback to DISPO after the final customer confirmation status is known.

Callback target in local development:

```text
http://localhost:8090/api/dispo/confirmation-status-updates
```

Callback payload:

```json
{
  "externalOrderId": "A-2001",
  "confirmationStatus": "CONFIRMED",
  "customerComment": null
}
```

Possible callback statuses:

```text
CONFIRMED
REJECTED
NO_RESPONSE
```

The callback is executed through a Camunda workflow. If the HTTP callback fails, Camunda creates a failed job and retries it according to the configured retry behavior.

---

## Confirmation Status Values

| Status        | Meaning                                                  |
| ------------- | -------------------------------------------------------- |
| `SENT`        | Message was sent and backend waits for customer response |
| `CONFIRMED`   | Customer confirmed the delivery window                   |
| `REJECTED`    | Customer rejected the delivery window                    |
| `NO_RESPONSE` | Customer did not respond before the deadline             |

---

## Communication Channels

| Channel | Meaning                                     |
| ------- | ------------------------------------------- |
| `EMAIL` | Confirmation link is sent by e-mail         |
| `SMS`   | Confirmation link is sent by SMS / SMS Mock |

The channel is selected by DISPO using the `communicationChannel` field.

---

## Duplicate Request Handling

The backend prevents unnecessary duplicate messages.

If DISPO sends the same request again and the relevant request data has not changed, the backend returns `200 OK` and does not create a new confirmation request.

Relevant comparison data includes:

* order data
* delivery date
* delivery window start
* delivery window end
* communication channel
* response deadline hours

If confirmation-relevant data changes, the previous request is marked inactive and a new confirmation request is created.

An unchanged active request is treated as a duplicate. Unchanged requests that have already reached `CONFIRMED` or `REJECTED` are also returned without creating a second request. After `NO_RESPONSE`, DISPO may send the same data again; in that case the backend creates a new active confirmation request and resets the order status to `SENT`.

---

## Customer Link Behavior

The customer link opens the frontend route:

```text
/confirmation/{token}
```

The frontend should use the token to load confirmation data from the backend.

If the customer already answered, the backend can still return the confirmation data together with the current status. This allows the frontend to show the customer what was already submitted.

If the request is no longer usable for submitting a new answer, the confirm/reject endpoint rejects the submission.

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
| `MISSING_DIGITAL_CONTACT`          |         422 | Required channel contact is missing   |
| `EMAIL_SENDING_FAILED`             |         502 | Confirmation e-mail could not be sent |
| `SMS_SENDING_FAILED`               |         502 | Confirmation SMS could not be sent    |
| `DISPO_CALLBACK_FAILED`            |         502 | DISPO callback could not be sent       |
| `CONFIRMATION_REQUEST_NOT_FOUND`   |         404 | Token does not exist                  |
| `CONFIRMATION_REQUEST_INACTIVE`    |         409 | Request is no longer active           |
| `CONFIRMATION_REQUEST_EXPIRED`     |         410 | Request has expired                   |
| `CUSTOMER_RESPONSE_ALREADY_EXISTS` |         409 | Customer already answered             |
| `NOT_FOUND`                        |         404 | Resource not found                    |
| `INTERNAL_ERROR`                   |         500 | Unexpected backend error              |

---

## Local End-to-End Test Flow

### 1. Start Infrastructure

```bash
docker compose up -d
```

### 2. Start Backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

or run `BackendApplication` in IntelliJ with:

```text
-Dspring.profiles.active=dev
```

### 3. Create EMAIL Confirmation Request

```http
POST http://localhost:8080/api/dispo/confirmation-requests
```

Use the EMAIL example request body.

Expected:

```http
201 Created
```

### 4. Open Mailpit

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

### 5. Test Preview Without Frontend

```http
GET http://localhost:8080/api/customer/confirmations/{token}
```

### 6. Confirm Without Frontend

```http
POST http://localhost:8080/api/customer/confirmations/{token}/confirm
```

Expected:

```http
204 No Content
```

### 7. Check Database

```sql
SELECT external_order_id, confirmation_status
FROM order_snapshot
WHERE external_order_id = 'A-2001';
```

Expected:

```text
CONFIRMED
```

### 8. Check DISPO Callback

```http
GET http://localhost:8090/api/dispo/confirmation-status-updates
```

Expected: a callback with status `CONFIRMED`.

### 9. Test SMS Flow

Create a request with:

```json
"communicationChannel": "SMS"
```

Then check the SMS Mock and use the token from the SMS message.

---

## Camunda Timeout

After a confirmation request is sent, the backend starts a Camunda timeout process.

The timeout duration is based on the request field:

```json
"responseDeadlineHours": 24
```

The backend stores this value in:

```text
confirmation_request.response_deadline_hours
```

Camunda receives this value as a duration and waits until the request expires.

If the customer does not answer before the deadline:

```text
confirmation_status = NO_RESPONSE
confirmation_request.active = false
```

After that, the backend starts a DISPO callback workflow with status `NO_RESPONSE`.

---

## Useful SQL Queries

### Check Order Status

```sql
SELECT
    external_order_id,
    confirmation_status,
    customer_name,
    customer_email,
    customer_phone_number,
    delivery_address,
    product,
    quantity_liters,
    price_display_text
FROM order_snapshot
ORDER BY id DESC;
```

### Check Confirmation Requests

```sql
SELECT
    os.external_order_id,
    cr.id AS confirmation_request_id,
    cr.active,
    cr.communication_channel,
    cr.response_deadline_hours,
    cr.sent_at,
    cr.expires_at,
    cr.token
FROM confirmation_request cr
JOIN order_snapshot os ON os.id = cr.order_snapshot_id
ORDER BY cr.id DESC;
```

### Check Customer Responses

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

### Check Order With Latest Request

```sql
SELECT
    os.external_order_id,
    os.confirmation_status,
    os.customer_name,
    os.customer_email,
    os.customer_phone_number,
    os.delivery_address,
    os.product,
    os.quantity_liters,
    os.price_display_text,
    cr.id AS confirmation_request_id,
    cr.active,
    cr.communication_channel,
    cr.response_deadline_hours,
    cr.sent_at,
    cr.expires_at
FROM order_snapshot os
JOIN confirmation_request cr ON cr.order_snapshot_id = os.id
ORDER BY cr.id DESC;
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

3. Show the delivery information, price (when present), and current confirmation status.
4. On the delivery date, optionally load tracking information and poll the driver-location endpoint.
5. Let the customer confirm or reject if the request is still open.
6. Submit one of:

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

The frontend should handle these statuses:

| Status        | Frontend behavior                                 |
| ------------- | ------------------------------------------------- |
| `SENT`        | Show confirmation/rejection buttons               |
| `CONFIRMED`   | Show that the customer already confirmed          |
| `REJECTED`    | Show that the customer already rejected           |
| `NO_RESPONSE` | Show that the deadline has passed / contact DISPO |

---

## Current MVP Limitations

* Real external DISPO system is represented by a local DISPO Mock.
* Real SMS provider is represented by a local SMS Mock.
* Mailpit is used for local SMTP testing; the production profile expects externally supplied SMTP credentials.
* WhatsApp integration is not implemented yet.
* Token is stored as plain text for MVP.
* Authentication/authorization is not part of the MVP.
* Frontend is developed separately.
* Full frontend/backend integration may still require frontend-side API wiring.
* Production deployment configuration is only prepared as an example.

---


## Summary

The backend supports the main MVP confirmation flow:

1. DISPO sends a confirmation request.
2. Backend validates and stores the request.
3. Backend sends a customer link via e-mail or SMS.
4. Customer confirms or rejects the delivery window.
5. Backend stores the response and updates the order status.
6. If the customer does not answer in time, Camunda marks the request as `NO_RESPONSE`.
7. Backend sends the final status back to DISPO through a retryable callback workflow.
