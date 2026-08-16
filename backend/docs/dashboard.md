# Dispatcher Dashboard

## Purpose

The dashboard API provides company-scoped tour overviews, filter options, order/request history, and manual resend. It is a read model over orders and confirmation requests; it does not expose persistence entities.

Exact response fields are defined by the dashboard DTOs and Swagger. This document focuses on query behavior and business interpretation.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/dispo/dashboard/tours` | Return a page of tours with their matching orders and status counts. |
| `GET` | `/api/dispo/dashboard/tour-numbers` | Return matching tour numbers for filter controls. |
| `GET` | `/api/dispo/dashboard/orders/{externalOrderId}` | Return order data and complete confirmation-request history. |
| `POST` | `/api/dispo/dashboard/orders/{externalOrderId}/resend` | Create and start a replacement confirmation request. |

All four operations use the resolved `CompanyContext`. An order belonging to another company is not visible or resendable through the current company context.

## Tour Overview

`GET /api/dispo/dashboard/tours` accepts these query parameters:

| Parameter | Behavior |
| --- | --- |
| `tourNumbers` | Optional set of exact tour numbers. Blank values are ignored. |
| `statuses` | Optional set of `ConfirmationStatus` values. |
| `search` | Optional case-insensitive partial search. |
| `dateFrom` | Inclusive ISO date. Defaults to the current date. |
| `dateTo` | Optional inclusive ISO date. |
| `page` | Zero-based page number. Negative values are normalized to `0`. |

`dateFrom` must not be after `dateTo`. An invalid range or invalid enum/date parameter returns `400` with the standard validation error envelope.

The page size is currently 20 tours. Pagination metadata contains the current page, page size, total elements, and total pages.

### Selection Rules

The tour overview:

- scopes orders to the current company;
- uses only the latest confirmation request for each order;
- excludes orders whose current order status is `OPEN`;
- applies the requested tour, date, status, and search filters; and
- groups matching rows by tour number, vehicle license plate, and delivery date.

The general `search` parameter matches tour number, vehicle license plate, external order ID, customer name, or delivery address. Comparison is case-insensitive and uses partial containment.

Tours are ordered by delivery date and tour number. Orders inside a tour are ordered by delivery date, delivery-window start/end, and external order ID.

### Response Interpretation

Each tour contains:

- tour number;
- vehicle license plate;
- delivery date;
- status counts; and
- its ordered list of matching orders.

Each order item contains its external ID, customer/address summary, delivery window, communication channel, confirmation status, and response deadline (`expiresAt`). Status counts aggregate the returned orders into `SENT`, `CONFIRMED`, `REJECTED`, and `NO_RESPONSE`.

## Tour Number Options

`GET /api/dispo/dashboard/tour-numbers` accepts `search`, `dateFrom`, and `dateTo`.

It follows the same company, current-date default, date-range validation, latest-request, and `OPEN`-exclusion rules as the tour overview. Its `search` parameter filters tour numbers only. Results are ordered by delivery date and tour number.

## Order Detail and History

`GET /api/dispo/dashboard/orders/{externalOrderId}` first resolves the order by both company ID and external order ID. A missing order returns `404`.

The result contains:

- `order`: current order/customer/tour data and order confirmation status;
- `currentRequest`: the newest confirmation request, or `null` when none exists; and
- `previousRequests`: all older requests, newest first.

All request delivery states are retained in the detail history. Requests are ordered by descending request ID, so the first request is current regardless of whether its delivery status is `PENDING`, `FAILED`, or `SENT`.

The request detail exposes a single display `status`:

- delivery `PENDING` is shown as `PENDING`;
- delivery `FAILED` is shown as `FAILED`; and
- delivery `SENT` is resolved to `SENT`, `CONFIRMED`, `REJECTED`, or `NO_RESPONSE` from activity and customer-response data.

This display status must not be confused with the order-level `ConfirmationStatus` or the persisted notification delivery status. See [Confirmation Workflow](confirmation-workflow.md#state-model).

## Manual Resend

`POST /api/dispo/dashboard/orders/{externalOrderId}/resend` accepts a communication channel and a positive response deadline of at most 168 hours.

The resend use case:

1. resolves the order inside the current company;
2. validates that the order has the contact needed by the selected channel;
3. rejects resend while the latest request is still `PENDING`;
4. deactivates and supersedes an active previous request;
5. keeps the previous delivery slot;
6. creates a new `PENDING` request with the selected channel/deadline; and
7. starts a new asynchronous delivery workflow.

The endpoint returns `202 Accepted` with `externalOrderId` and order status `OPEN`. It does not wait for the notification provider.

## Implementation Sources

- Controller and HTTP mapping: `adapter/in/web/overview/DashboardController.java`
- Response mapping: `adapter/in/web/overview/dto/`
- Filter normalization: `application/service/overview/`
- Query behavior: `adapter/out/persistence/TourOverviewQueryAdapter.java`
- Detail history: `adapter/out/persistence/ConfirmationDetailQueryAdapter.java`
- Resend rules: `application/service/confirmation/ResendConfirmationRequestService.java`
- Behavior verification: overview service/controller tests and PostgreSQL QueryDSL integration tests

## Related Documentation

- [Architecture](architecture.md)
- [Confirmation Workflow](confirmation-workflow.md)
- [API](api.md)
- [Configuration](configuration.md)
