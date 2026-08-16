# Confirmation Workflow

## Purpose

The confirmation workflow separates accepting a DISPO request from delivering the customer message and waiting for the customer's decision. The initial HTTP call persists work and starts a durable Camunda process; message delivery and later callbacks happen asynchronously.

For exact BPMN elements, retry counts, and timers, use `src/main/resources/processes/` and the Camunda integration tests as the source of truth.

## State Model

The backend tracks two related state dimensions.

### Order Confirmation Status

| Status | Meaning |
| --- | --- |
| `OPEN` | The order has accepted confirmation work that is not yet successfully delivered. |
| `SENT` | A customer message was delivered and the request is waiting for a response. |
| `CONFIRMED` | The customer accepted the delivery window. |
| `REJECTED` | The customer rejected the delivery window. |
| `NO_RESPONSE` | The active request reached its response deadline without a response. |

### Notification Delivery Status

Each `ConfirmationRequest` separately records whether the customer notification is `PENDING`, `SENT`, or `FAILED`.

This distinction matters at the API boundary: a newly accepted DISPO request returns the order status `OPEN`, while the related request starts with delivery status `PENDING`. `OPEN` does not mean that the customer message was delivered.

The domain enums and their tests are authoritative if these values change.

## Creating or Updating a Request

`POST /api/dispo/confirmation-requests` resolves the company first, validates the requested communication channel and delivery data, and then coordinates the order and its latest confirmation request.

At a high level, the create use case:

- creates an order in `OPEN` state and a `PENDING` confirmation request when no order exists;
- avoids starting a second workflow for unchanged work that is already pending;
- reuses an unchanged request whose successful result is still applicable;
- updates an existing pending request when its relevant input changes;
- supersedes an active sent request before creating a replacement; and
- creates a new pending request after a failed delivery, no response, or a relevant change that requires another customer request.

The exact comparison rules belong to `CreateConfirmationRequestService` and its tests. They are intentionally not duplicated as a decision matrix here.

## Asynchronous Notification Delivery

A newly created `ConfirmationRequest` is persisted with:

- delivery status `PENDING`;
- no `sentAt` or `expiresAt` timestamp; and
- `active = false` until delivery succeeds.

The Camunda process invokes the notification use case for the selected `EMAIL`, `SMS`, or `WHATSAPP` channel.

On successful delivery, the application:

1. records `sentAt`;
2. calculates and stores `expiresAt`;
3. changes delivery status to `SENT`;
4. activates the request; and
5. changes the order status to `SENT`.

Retryable provider failures remain workflow-owned. A permanent failure, or exhaustion of the delivery path, changes the request delivery status to `FAILED`, leaves it inactive, and returns the order to `OPEN`. Because delivery happens after the HTTP request, these failures are observed through persisted/dashboard state rather than as a synchronous `502` from the create endpoint.

## Response Deadline

DISPO supplies `responseDeadlineHours` for each request. The effective deadline is calculated only after the customer message is successfully delivered:

```text
min(sentAt + responseDeadlineHours, delivery window start)
```

Delivery-window timestamps are interpreted in `Europe/Berlin`. A message cannot be sent when the delivery window has already started. Camunda waits until the stored absolute `expiresAt` value rather than recalculating the deadline independently.

## Customer Link and Response

Notification adapters create a frontend link with the form:

```text
{frontend-url}/confirmation/{token}
```

The frontend uses the opaque token to load the preview and submit `CONFIRM` or `REJECT`. The token is valid only for the latest confirmation request of its order; older requests remain in history, but their links return `404 Not Found`. The latest request can still be previewed after it becomes inactive, including `NO_RESPONSE`. A response is accepted only when that latest request is active, has not expired, and has no existing customer response.

When a response is accepted, the application:

1. locks the relevant order;
2. stores the response and optional comment;
3. changes the order to `CONFIRMED` or `REJECTED`;
4. deactivates the request;
5. correlates the running workflow with the final status; and
6. attempts a channel-specific acknowledgement to the customer.

Failure of the acknowledgement is logged and does not roll back an already accepted customer decision.

## No Response

If the stored response deadline is reached first, the workflow verifies that the request is still active and expired, deactivates it, and changes the order to `NO_RESPONSE`. The same final callback path is then used as for a customer decision.

## Supersession and Resend

Replacing or manually resending an active request first deactivates it and correlates its workflow with a superseded message. A superseded workflow ends without sending a final DISPO status callback for the obsolete request.

The dispatcher resend endpoint creates a new `PENDING` request using the previous delivery slot and the requested channel/deadline. Resend is rejected while the latest request is still `PENDING`, because notification delivery is already in progress.

## DISPO Status Callback

Final states `CONFIRMED`, `REJECTED`, and `NO_RESPONSE` are sent to the callback URL stored for the request's company. Callback execution is asynchronous and retryable under Camunda. The application callback use case loads the persisted order before building the external request.

Exact callback process structure and retry behavior are defined by the current BPMN resources and workflow integration tests.

## Concurrency and Idempotency

Workflow jobs may be executed more than once. The application protects critical transitions by:

- locking the order for notification, response, timeout, and failure handling;
- treating an already-sent request as an idempotent delivery success;
- treating an already-failed request as an idempotent failure result;
- allowing only one customer response per confirmation request; and
- correlating workflows by the confirmation-request ID as business key.

Changes to delivery, timeout, resend, or callback behavior must verify duplicate execution, partial failure, and tenant isolation in the relevant tests.

## Related Documentation

- [Architecture](architecture.md)
- [API](api.md)
- [Dashboard](dashboard.md)
- [Configuration](configuration.md)
