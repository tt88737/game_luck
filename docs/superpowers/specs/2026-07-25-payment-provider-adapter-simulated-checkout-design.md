# Payment Provider Adapter And Simulated Checkout Design

**Date:** 2026-07-25
**Phase:** 43
**Status:** Approved design

## Goal

Introduce a provider-neutral payment boundary that exercises real HTTP payment-session and signed-webhook flows without binding the platform to a real payment supplier. Deliver a hosted simulated checkout that can produce payment success, payment failure, cancellation, refund, and chargeback events while reusing the existing purchase fulfillment and reversal review workflows.

## Scope

Phase 43 includes:

- a provider adapter contract and registry inside `gameluck-payment`;
- persistent payment sessions and immutable webhook event records;
- a hosted simulated checkout with signed HTTP webhooks;
- C-side payment-session creation and status queries;
- Admin payment-session and webhook-event visibility;
- permission-protected retry for failed webhook processing;
- integration with existing purchase fulfillment, refund recovery, chargeback recovery, risk escalation, and reversal review;
- backend, Admin, H5, SQL idempotency, and browser/runtime acceptance.

Phase 43 excludes real provider SDKs and credentials, card data collection, partial refunds, repeated partial chargebacks, scheduled reconciliation, message queues, and a separate payment microservice.

## Architecture

The provider boundary remains in `gameluck-payment` but is isolated behind explicit interfaces:

- `PaymentProviderAdapter` creates and queries provider sessions and verifies webhook signatures.
- `PaymentProviderRegistry` resolves an enabled adapter by provider code and rejects unknown or disabled providers.
- `SimulatedPaymentProviderAdapter` is the first runnable implementation.
- `PaymentWebhookService` persists the original provider event before invoking business processing.
- Existing payment event, purchase fulfillment, refund, and chargeback services remain the only paths that mutate orders, wallets, turnover tasks, member risk, and reversal cases.

Adapters must not call wallet mappers or directly change purchase-order state.

```text
H5 creates a purchase order
  -> platform creates a payment session
  -> H5 redirects to hosted checkout URL
  -> member selects a simulated result
  -> simulated provider sends a signed webhook
  -> platform verifies and stores the raw event
  -> existing payment/reversal services process it
  -> H5 queries the resulting order/session state
```

The browser return URL is informational only. A purchase becomes paid only after a valid webhook is processed.

## Persistence

### Payment Session

`gl_payment_session` stores:

- tenant ID, platform session number, purchase order ID and number, and member ID;
- provider code and provider session number;
- pay currency and amount copied from the purchase order;
- hosted checkout URL;
- `CREATED`, `PENDING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, or `EXPIRED` status;
- creation request key, expiration time, completion time, version, and audit timestamps.

Required uniqueness:

- `(tenant_id, session_no)`;
- `(tenant_id, request_key)`;
- `(tenant_id, provider_code, provider_session_no)`.

Multiple sessions may exist for one unpaid order after earlier sessions expire or fail. Only one nonterminal session may be active for an order at a time.

### Webhook Event

`gl_payment_webhook_event` stores:

- tenant ID, provider code, provider event ID, and event type;
- related provider session, platform session, and purchase order numbers;
- raw request body, signature digest, and received timestamp;
- `RECEIVED`, `PROCESSED`, `FAILED`, or `IGNORED` processing status;
- failure reason, processing count, last processing time, and audit timestamps.

Required uniqueness:

- `(tenant_id, provider_code, provider_event_id)`.

The raw body is immutable. Retry updates processing metadata only.

## State And Event Rules

Provider events map as follows:

| Provider event | Platform behavior |
| --- | --- |
| `PAYMENT_SUCCEEDED` | Complete payment and execute purchase fulfillment once |
| `PAYMENT_FAILED` | Mark the payment attempt failed without wallet mutation |
| `PAYMENT_CANCELLED` | Cancel the payment attempt without wallet mutation |
| `REFUND_SUCCEEDED` | Enter the existing refund recovery workflow |
| `CHARGEBACK_CREATED` | Enter the existing chargeback recovery and risk workflow |

Rules:

- A terminal order cannot regress because of a late event.
- Replaying the same provider event returns a successful acknowledgement without duplicate business effects.
- Different provider event IDs that express the same result are guarded by the existing order and payment-event state machines.
- Session amount and currency must equal the purchase-order snapshot before any business transition.
- A failed business transaction leaves the webhook event retryable as `FAILED` and rolls back downstream changes.
- Refund and chargeback events reuse Phase 41 atomic recovery and Phase 42 review disposition behavior.

## APIs

### Client

- `POST /api/client/purchase/orders/{orderNo}/payment-sessions`
- `GET /api/client/purchase/payment-sessions/{sessionNo}`

Session creation requires a request key and returns the session number, status, checkout URL, and expiration time. Replaying the same request returns the original session.

### Provider

- `GET /payment/simulated/checkout/{providerSessionNo}`
- `POST /payment/webhooks/{providerCode}`

The simulated checkout offers:

- payment success, payment failure, and payment cancellation while pending;
- refund success and chargeback creation after successful payment;
- replay of the most recent event for explicit idempotency verification.

Each new action generates a unique provider event ID and invokes the same signed webhook endpoint used by future real adapters. Terminal actions are disabled when invalid for the current provider-session state.

### Admin

Admin provides:

- payment-session list and detail;
- webhook-event list and detail, including raw payload, signature result, processing state, and failure reason;
- permission-protected retry for `FAILED` events.

Admin cannot edit event payloads, amounts, currencies, or event types. Retry actions require separate permissions and operation logs.

## Webhook Security

Webhooks use `HMAC-SHA256` over:

```text
timestamp + "." + rawBody
```

The endpoint must:

- read and verify the exact raw request bytes before JSON mapping;
- enforce a configurable timestamp tolerance;
- use constant-time signature comparison;
- require an enabled provider configuration;
- reject invalid signatures with HTTP 401;
- return HTTP 200 for accepted events and idempotent replays;
- avoid returning internal exception details.

Provider secrets are injected from backend configuration. Plaintext secrets must not be stored in database rows, logs, API responses, frontend bundles, or audit snapshots. Admin may display only a non-reversible digest or masked identifier.

## Failure Handling

- Provider session creation failure leaves the purchase order unpaid and permits retry with the same request key.
- Expired sessions cannot produce a successful payment and may be replaced by a new session.
- Invalid signature, stale timestamp, payload tampering, or amount/currency mismatch never advances the order.
- A webhook processing exception marks the event `FAILED`; retry uses the same immutable event and provider event ID.
- A late or contradictory event is recorded as `IGNORED` with an operator-readable reason.
- Provider responses and logs must not expose secrets or stack traces.

## User Experience

H5 redirects from the purchase flow to the hosted simulated checkout and returns to an order-result view. The result view polls only platform session/order status and never trusts query-string status values.

The simulated checkout is an operational test surface, not an Admin form. It clearly displays order number, amount, currency, provider session, expiration, current state, allowed actions, and the latest webhook result. It must work on desktop and mobile.

Admin session and webhook pages follow the existing dense operational table/detail patterns, with status filters, exact identifiers, timestamps, failure reasons, and links back to the purchase order and reversal review where applicable.

## Acceptance

Automated and runtime acceptance must cover:

- idempotent session creation, expiration, unknown providers, and disabled providers;
- valid HMAC signatures, invalid signatures, stale timestamps, and tampered raw bodies;
- one-time fulfillment after payment success;
- no wallet mutation after payment failure or cancellation;
- same-event replay, different-event duplicate success, and out-of-order events;
- refund routing into existing refund recovery;
- chargeback routing into recovery, risk escalation, and the Phase 42 review workbench;
- failed webhook processing followed by audited successful retry;
- SQL imported twice without duplicate schema, menu, permission, or dictionary records;
- focused and cross-module backend regression;
- Admin and H5 production-equivalent builds;
- desktop and mobile browser acceptance for checkout, H5 result, and Admin detail views;
- backend, Admin, and H5 runtime endpoints returning HTTP 200.

## Implementation Constraints

- Use TDD for persistence, adapter, signature, idempotency, and state-machine behavior.
- Preserve tenant isolation in every query and unique key.
- Use guarded updates or row locks for session and webhook transitions.
- Preserve the shared dirty `main` worktree and do not create Git commits unless explicitly requested.
- Do not add a message queue or split a payment microservice in this phase.
