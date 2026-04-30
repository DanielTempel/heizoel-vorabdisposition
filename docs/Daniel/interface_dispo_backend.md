# Interface DISPO ↔ Backend

## 1. Purpose

This document defines the REST API contract between the external DISPO system and the Rückbestätigungs-Backend.

The DISPO system is the leading system for order data and delivery planning. The backend does not create business orders independently. Instead, it receives the order data required for the digital confirmation process, stores a local snapshot, creates a confirmation request, sends an e-mail to the customer and manages the confirmation status.

## 2. System Boundary

### DISPO System

The DISPO system is responsible for:

- managing existing orders
- delivery planning
- assigning delivery dates and delivery windows
- dispatcher workflow
- manual start of the confirmation process

### Rückbestätigungs-Backend

The backend is responsible for:

- receiving confirmation requests from DISPO
- validating incoming order data
- storing a local order snapshot
- creating a confirmation request
- generating token-based action URLs
- sending the customer notification by e-mail
- managing the confirmation status
- storing customer responses
- handling expiration after 24 hours

## 3. Process Start

The confirmation process is started manually by the dispatcher in DISPO.

The dispatcher works in the DISPO system and clicks a button such as:

```text
Rückbestätigung senden
````

After this action, DISPO calls the backend via REST API.

In the prototype, the DISPO call may be simulated through Swagger, Postman or a mock frontend.

## 4. Endpoint Overview

```http
POST /api/dispo/confirmation-requests
```

This endpoint creates a new confirmation request for an existing order and sends the e-mail synchronously.

## 5. Request DTO

### 5.1 Request JSON

```json
{
  "externalOrderId": "A-1024",
  "customerName": "Max Müller",
  "customerEmail": "daniel@example.com",
  "deliveryAddress": "Beispielstraße 12, 97070 Würzburg",
  "product": "Heizöl",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00"
}
```

### 5.2 Field Description

| Field                 |      Type | Required | Description                                             |
| --------------------- | --------: | -------: | ------------------------------------------------------- |
| `externalOrderId`     |  `string` |      yes | Order identifier from the DISPO system.                 |
| `customerName`        |  `string` |      yes | Name of the customer.                                   |
| `customerEmail`       |  `string` |      yes | E-mail address used for the confirmation request.       |
| `deliveryAddress`     |  `string` |      yes | Full delivery address as a single text field.           |
| `product`             |  `string` |      yes | Product to be delivered, e.g. Heizöl.                   |
| `quantityLiters`      | `integer` |      yes | Ordered quantity in liters.                             |
| `deliveryDate`        |    `date` |      yes | Planned delivery date in ISO format `YYYY-MM-DD`.       |
| `deliveryWindowStart` |    `time` |      yes | Start of the planned delivery window in format `HH:mm`. |
| `deliveryWindowEnd`   |    `time` |      yes | End of the planned delivery window in format `HH:mm`.   |

### 5.3 Fields intentionally not included in v1

The following fields are intentionally not part of the first API version:

| Field               | Reason                                                                                |
| ------------------- | ------------------------------------------------------------------------------------- |
| `externalTourId`    | Tour data is not required for the MVP. The confirmation process works on order level. |
| `customerNumber`    | Not required for the confirmation process in the prototype.                           |
| `pricePer100Liters` | Not required for MVP confirmation.                                                    |
| `dispatcherId`      | It is not guaranteed that DISPO provides this information.                            |
| `dispatcherName`    | It is not guaranteed that DISPO provides this information.                            |

## 6. Response DTO

### 6.1 Success Response

The backend returns `201 Created` only if:

* the incoming request was valid
* a confirmation request was created
* token-based action URLs were generated
* the e-mail was sent successfully
* the confirmation status was set to `SENT`

```http
201 Created
```
### 6.1 Success Response

The backend returns `201 Created` only if:

* the incoming request was valid
* a confirmation request was created
* token-based action URLs were generated internally
* the e-mail was sent successfully
* the confirmation status was set to `SENT`

```java
201 Created
{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "SENT"
}
```

The response does not expose the generated customer action URLs. These URLs are used internally by the backend notification service to build the e-mail content.

The response also does not include sentAt or expiresAt in API version 1. The response only confirms that the request was successfully created and sent.

### 6.2 Field Description
Field	Type	Description
confirmationRequestId	UUID	Internal identifier of the created confirmation request in the backend.
externalOrderId	string	Order identifier from DISPO. Returned for correlation with the DISPO order.
confirmationStatus	string	Current confirmation status. In this response normally SENT.
### 6.3 Notes on internal action URLs

The backend generates token-based action URLs internally, for example:

/confirmation/{token}?action=confirm
/confirmation/{token}?action=reject

These URLs are inserted into the e-mail as visible customer action buttons.

The action URLs are not part of the DISPO response because DISPO does not need to process customer-facing links. The links are only relevant for the customer notification and the customer confirmation page.

Opening an action URL does not directly change the confirmation status. It opens a minimal customer confirmation page. The status is changed only after the customer submits the final action on that page.

### 6.4 Later status update to DISPO

After the customer has confirmed or rejected the proposed delivery window, the backend informs DISPO through a callback/webhook mechanism.

The callback is separate from the initial POST /api/dispo/confirmation-requests response.

Target concept:
POST /api/dispo/confirmation-status-updates
Content-Type: application/json

Example callback payload:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "CONFIRMED",
  "customerComment": null
}

Example for rejection:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "REJECTED",
  "customerComment": "Bitte erst ab 15 Uhr."
}

In the prototype, the real DISPO callback is represented by a mock implementation. The mock callback may log the status update or store it in a mock DISPO component.

## Message Types

The interface distinguishes between two different message types.

### Message Type 1: Confirmation request creation response

This message is returned immediately after DISPO calls:

```http
POST /api/dispo/confirmation-requests

It confirms that the backend created the confirmation request and successfully sent the e-mail.

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "SENT"
}

This message does not contain customerComment, because no customer response exists at this point.

Message Type 2: Customer response status update

This message is sent later through a callback/webhook after the customer has responded.

POST /api/dispo/confirmation-status-updates

Example for confirmation:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "CONFIRMED",
  "customerComment": null
}

Example for rejection:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "REJECTED",
  "customerComment": "Bitte erst ab 15 Uhr."
}

customerComment is included only in customer-related status updates. It is optional and is not automatically interpreted by the backend.

## 7. Action URLs

The e-mail sent to the customer contains visible action buttons, for example:

```text
Liefertermin bestätigen
Termin passt nicht
```

Technically, these buttons are token-based action links.

Example:

```text
http://localhost:3000/confirmation/abc-token?action=confirm
http://localhost:3000/confirmation/abc-token?action=reject
```

Opening such a link does not directly change the confirmation status.

Instead, the link opens a minimal customer confirmation page. On this page, the customer sees the relevant delivery data and confirms the selected action explicitly.

### 7.1 Confirm action flow

```text
Customer clicks "Liefertermin bestätigen"
↓
Minimal customer page opens
↓
Action "confirm" is preselected
↓
Customer sees delivery data
↓
Customer clicks final confirmation button
↓
Backend updates status to CONFIRMED
```

### 7.2 Reject action flow

```text
Customer clicks "Termin passt nicht"
↓
Minimal customer page opens
↓
Action "reject" is preselected
↓
Customer sees delivery data
↓
Customer may enter an optional comment
↓
Customer submits rejection
↓
Backend updates status to REJECTED
```

### 7.3 Reason for intermediate confirmation page

The backend must not change the confirmation status directly through a simple `GET` request.

Reason:

E-mail clients, security software or link scanners may automatically open links. If opening a link directly changed the status, an order could be confirmed or rejected without conscious customer action.

Therefore:

```text
GET action URL = open customer page
POST final action = update status
```

## 8. Validation Rules

### 8.1 Required fields

The following fields must be present and non-empty:

```text
externalOrderId
customerName
customerEmail
deliveryAddress
product
quantityLiters
deliveryDate
deliveryWindowStart
deliveryWindowEnd
```

### 8.2 Field validation

| Rule ID   | Field                 | Rule                                                      |
| --------- | --------------------- | --------------------------------------------------------- |
| `VAL-001` | `externalOrderId`     | Must not be blank.                                        |
| `VAL-002` | `customerName`        | Must not be blank.                                        |
| `VAL-003` | `customerEmail`       | Must not be blank and must be a valid e-mail address.     |
| `VAL-004` | `deliveryAddress`     | Must not be blank.                                        |
| `VAL-005` | `product`             | Must not be blank.                                        |
| `VAL-006` | `quantityLiters`      | Must be greater than `0`.                                 |
| `VAL-007` | `deliveryDate`        | Must be a valid date in format `YYYY-MM-DD`.              |
| `VAL-008` | `deliveryWindowStart` | Must be a valid time in format `HH:mm`.                   |
| `VAL-009` | `deliveryWindowEnd`   | Must be a valid time in format `HH:mm`.                   |
| `VAL-010` | delivery window       | `deliveryWindowStart` must be before `deliveryWindowEnd`. |

### 8.3 Optional implementation decision

For the MVP, the backend does not validate whether the delivery date is in the future. This can be added later if required.

## 9. Error Responses

All error responses use one common structure.

```json
{
  "errorCode": "ERROR_CODE",
  "message": "Human-readable error message.",
  "externalOrderId": "A-1024"
}
```

Field	Type	Required	Description
errorCode	string	yes	Technical error code.
message	string	yes	Human-readable error message.
externalOrderId	string	no	DISPO order id, if available in the request.
9.1 Validation Error

Returned when the request contains invalid or missing data.

400 Bad Request

Example: missing e-mail address.

{
  "errorCode": "VALIDATION_ERROR",
  "message": "Customer e-mail is required for digital confirmation.",
  "externalOrderId": "A-1024"
}

Example: invalid e-mail address.

{
  "errorCode": "VALIDATION_ERROR",
  "message": "Customer e-mail must be a valid e-mail address.",
  "externalOrderId": "A-1024"
}

Example: invalid delivery window.

{
  "errorCode": "VALIDATION_ERROR",
  "message": "Delivery window start must be before delivery window end.",
  "externalOrderId": "A-1024"
}
9.2 E-Mail Sending Failed

Returned when the confirmation request could not be sent by e-mail.

502 Bad Gateway
{
  "errorCode": "EMAIL_SENDING_FAILED",
  "message": "The confirmation e-mail could not be sent.",
  "externalOrderId": "A-1024"
}

In this case:

no 201 Created response is returned
the order confirmation status is not changed to SENT
the failed sending attempt may be stored internally as SEND_FAILED
no customer response can be submitted for this failed request
9.3 Internal Error

Returned when an unexpected technical error occurs.

500 Internal Server Error
{
  "errorCode": "INTERNAL_ERROR",
  "message": "An unexpected technical error occurred.",
  "externalOrderId": "A-1024"
}

Internal technical details are not exposed through the API response. They are stored in backend logs.

## 10. Duplicate and repeated requests

DISPO may send more than one request for the same `externalOrderId`.

The backend distinguishes between two cases.

### 10.1 Duplicate request with unchanged confirmation data

If a request for the same `externalOrderId` is received and the confirmation-relevant data is unchanged, the backend treats the request as a duplicate.

In this case:

- no new confirmation request is created
- no second e-mail is sent
- the existing active confirmation request is returned

```java
200 OK
{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "SENT"
}
```
10.2 New request with changed confirmation data

If a request for the same externalOrderId is received but the confirmation-relevant data has changed, the backend treats it as a new confirmation request.

Examples of confirmation-relevant changes:

changed delivery date
changed delivery window start
changed delivery window end
changed delivery address
changed quantity

In this case:

previous active confirmation requests for this order are invalidated
a new confirmation request is created
a new e-mail is sent
the new confirmation request is returned
201 Created
{
  "confirmationRequestId": "9bd8b404-2cf5-4210-a2fb-84f9a97d63ff",
  "externalOrderId": "A-1024",
  "confirmationStatus": "SENT"
}

The API v1 does not return 409 Conflict for duplicate active requests.



### 9. Business Rule добавить

```markdown
| `BR-022` | If DISPO sends the same request for the same `externalOrderId` with unchanged confirmation-relevant data, the backend treats it as duplicate and does not send a second e-mail. |
| `BR-023` | If DISPO sends a request for the same `externalOrderId` with changed confirmation-relevant data, the backend invalidates previous active requests and creates a new confirmation request. |


## 10. Confirmation Status Values

The API uses English enum values. The user interface may display German labels.

| API Value     | German Label      | Meaning                                                                                |
| ------------- | ----------------- | -------------------------------------------------------------------------------------- |
| `SENT`        | versendet         | Confirmation request was successfully sent and the system waits for customer response. |
| `CONFIRMED`   | bestätigt         | Customer confirmed the proposed delivery window within the valid response period.      |
| `REJECTED`    | abgelehnt         | Customer rejected the proposed delivery window within the valid response period.       |
| `NO_RESPONSE` | keine Rückmeldung | No valid customer response was received within 24 hours after successful e-mail sending. |

## 11. Business Rules

| Rule ID  | Rule |
| -------- | ---- |
| `BR-001` | The order already exists in DISPO. |
| `BR-002` | DISPO is the leading system for order data and delivery planning. |
| `BR-003` | The backend stores only a local snapshot of the order data required for confirmation. |
| `BR-004` | The confirmation process is started manually by the dispatcher in DISPO. |
| `BR-005` | DISPO calls the backend directly via REST API. |
| `BR-006` | A confirmation request always refers to exactly one order and one concrete delivery window. |
| `BR-007` | Tour data is not part of API v1. |
| `BR-008` | The status `SENT` is set only after successful technical e-mail sending. |
| `BR-009` | If e-mail sending fails, the API returns an error and the order confirmation status is not changed to `SENT`. |
| `BR-010` | The e-mail contains token-based action buttons. |
| `BR-011` | Action links open a minimal customer confirmation page. |
| `BR-012` | Opening an action link does not directly change the confirmation status. |
| `BR-013` | The confirmation status is changed only after a final submit on the customer page. |
| `BR-014` | Only one valid customer response is allowed per confirmation request. |
| `BR-015` | Only the latest active request can update the current order confirmation status. |
| `BR-016` | Old, inactive or replaced links must not update the current order status. |
| `BR-017` | Customer comments are optional. |
| `BR-018` | Customer comments are stored but not automatically interpreted. |
| `BR-019` | The system does not perform automatic tour planning, route optimization or replanning. |
| `BR-020` | If the customer rejects or does not respond, the dispatcher decides manually how to proceed. |
| `BR-021` | After a customer response, the backend informs DISPO through a callback/webhook status update. |
| `BR-022` | If DISPO sends the same request for the same `externalOrderId` with unchanged confirmation-relevant data, the backend treats it as a duplicate and does not send a second e-mail. |
| `BR-023` | If DISPO sends a request for the same `externalOrderId` with changed confirmation-relevant data, the backend invalidates previous active requests and creates a new confirmation request. |

## 12. Sequence: Successful Confirmation Request

```text
DISPO
  |
  | POST /api/dispo/confirmation-requests
  v
Backend
  |
  | validate request
  | store/update order snapshot
  | create confirmation request
  | generate token-based action URLs
  | send e-mail
  v
Notification Service
  |
  | e-mail successfully sent
  v
Backend
  |
  | set status SENT
  | set sentAt
  | set expiresAt = sentAt + 24h
  v
DISPO
  |
  | 201 Created
  v
Dispatcher
```

## 13. Sequence: E-Mail Sending Failed

```text
DISPO
  |
  | POST /api/dispo/confirmation-requests
  v
Backend
  |
  | validate request
  | create confirmation request
  | generate token-based action URLs
  | try to send e-mail
  v
Notification Service
  |
  | e-mail sending failed
  v
Backend
  |
  | order status remains OPEN
  | request may be stored as SEND_FAILED
  | no 24h timer starts
  v
DISPO
  |
  | 502 Bad Gateway
  v
Dispatcher
```

## 14. Assumptions

| ID      | Assumption                                                                                 |
| ------- | ------------------------------------------------------------------------------------------ |
| `A-001` | DISPO can call the backend via REST API.                                                   |
| `A-002` | The dispatcher starts the process manually in DISPO.                                       |
| `A-003` | In the prototype, DISPO can be simulated through Swagger, Postman or a mock frontend.      |
| `A-004` | The backend receives all order data required for the customer notification in one request. |
| `A-005` | E-mail is the primary communication channel in the MVP.                                    |
| `A-006` | SMS and WhatsApp are possible future channels but are not implemented in the MVP.          |
| `A-007` | The backend sends e-mail synchronously during the DISPO request.                           |
| `A-008` | The customer response deadline is 24 hours after successful e-mail sending.                |
| `A-009` | The frontend base URL is configurable.                                                     |
| `A-010` | The mail sender configuration is provided through environment variables.                   |

## 15. Open Questions

| ID      | Question                                                                                     |
| ------- | -------------------------------------------------------------------------------------------- |
| `Q-001` | Which exact fields can a real DISPO system provide?                                          |
| `Q-002` | Should the backend later send status updates back to DISPO through a callback?               |
| `Q-003` | Should SMS or WhatsApp be implemented after the e-mail MVP?                                  |
| `Q-004` | Should a reminder after 12 hours be implemented or only documented as an optional extension? |
| `Q-005` | Should failed e-mail attempts be visible to the dispatcher in the portal?                    |
| `Q-006` | Should manually confirmed phone cases be supported in the backend?                           |

## 16. API Contract Summary

### Request

```http
POST /api/dispo/confirmation-requests
Content-Type: application/json
```

```json
{
  "externalOrderId": "A-1024",
  "customerName": "Max Müller",
  "customerEmail": "daniel@example.com",
  "deliveryAddress": "Beispielstraße 12, 97070 Würzburg",
  "product": "Heizöl",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-12",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00"
}
```

### Success Response

```http
201 Created
```

```json
{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "SENT",
  "confirmActionUrl": "http://localhost:3000/confirmation/abc-token?action=confirm",
  "rejectActionUrl": "http://localhost:3000/confirmation/abc-token?action=reject",
  "sentAt": "2026-06-12T08:15:00Z",
  "expiresAt": "2026-06-13T08:15:00Z"
}
```

### Main Error Responses

```text
400 VALIDATION_ERROR
422 MISSING_DIGITAL_CONTACT
502 EMAIL_SENDING_FAILED
```

````
## 13. Callback / Webhook to DISPO

After the customer has submitted a response, the backend updates the internal confirmation status and informs DISPO through a callback/webhook.

In the prototype, the real DISPO callback is represented by a mock implementation. The mock may log the status update or store it in a mock DISPO component.

### 13.1 Callback Endpoint

The following endpoint represents the target DISPO callback interface:

```http
POST /api/dispo/confirmation-status-updates
Content-Type: application/json
13.2 Callback Payload

Example for a confirmed delivery window:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "CONFIRMED",
  "customerComment": null
}

Example for a rejected delivery window:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "REJECTED",
  "customerComment": "Bitte erst ab 15 Uhr."
}

Example for no customer response within 24 hours:

{
  "confirmationRequestId": "7f7a1e0e-5b7d-4d2c-9c6a-1f2bb9db42c1",
  "externalOrderId": "A-1024",
  "confirmationStatus": "NO_RESPONSE",
  "customerComment": null
}
13.3 Callback Field Description
Field	Type	Required	Description
confirmationRequestId	UUID	yes	Internal identifier of the confirmation request in the backend.
externalOrderId	string	yes	Order identifier from DISPO.
confirmationStatus	string	yes	New confirmation status. Possible values: CONFIRMED, REJECTED, NO_RESPONSE.
customerComment	string	no	Optional customer comment. Usually only relevant for REJECTED.
13.4 Callback Failure Handling

If the callback to DISPO fails, the customer response remains valid in the backend.

The backend must not roll back the customer response only because the callback failed.

In this case:

the confirmation status remains stored in the backend
the failed callback attempt is logged
a retry mechanism may be added later
the failed callback can be shown as a technical integration issue

The customer response is the source event. The callback is only the integration notification to DISPO.