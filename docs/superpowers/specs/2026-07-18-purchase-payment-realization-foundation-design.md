# Purchase Payment Realization Foundation Design

## Goal

Prepare the current simulated C-side purchase flow for real payment gateway integration without connecting a third-party provider yet.

This phase turns "pay and immediately credit" into a gateway-ready order/event model. The local H5 demo must continue to work, but the backend should gain explicit order states, provider references, callback/event idempotency, and a single service path for "payment succeeded, now grant wallet rewards".

## Scope

This phase includes:

- Extend `gl_purchase_order` with provider/session/callback-facing fields.
- Add stable purchase order statuses:
  - `CREATED`
  - `PENDING`
  - `PAID`
  - `CREDITED`
  - `FAILED`
  - `CANCELLED`
  - `REFUNDED`
  - `CHARGEBACK`
- Add a payment event table for callback/idempotency records.
- Add a backend service that applies payment events idempotently.
- Refactor C-side simulated purchase to create an order and then apply a simulated `PAY_SUCCESS` event through the same service that a future real callback will use.
- Keep wallet credit idempotency keys stable enough to avoid duplicate wallet grants on repeated callbacks.
- Add focused tests for repeated events, conflicting idempotency, failure events, and existing C-side purchase behavior.
- Add SQL migration statements to `backend/script/sql/gameluck_wallet.sql`.

This phase does not include:

- Stripe, PayPal, crypto, bank-card, or any real provider SDK.
- Hosted checkout page rendering.
- Webhook signature verification.
- Refund wallet reversal logic.
- Chargeback wallet clawback logic.
- New Admin purchase order page.
- Frontend UI redesign.

## Current State

`ClientPurchaseService.pay(...)` currently:

1. Checks the client token and purchase idempotency key.
2. Loads the offer and grant items.
3. Inserts a `gl_purchase_order` row with status `PENDING`.
4. Immediately marks it `PAID`.
5. Snapshots grant items and credits the wallet.
6. Marks the order `CREDITED`.

That is fine for a demo, but a real provider needs a durable pending order, provider order/session identifiers, callback idempotency, and a way to process repeated callbacks without double-crediting.

## Target Model

### Purchase Order

Add fields to `gl_purchase_order`:

| Field | Purpose |
| --- | --- |
| `provider_code` | Payment provider code, `SIMULATED` in this phase. |
| `provider_order_no` | Provider-side order/session id. Unique per tenant/provider when present. |
| `payment_session_no` | Internal payment session id for checkout/callback correlation. |
| `callback_event_key` | Last processed callback/event idempotency key. |
| `cancel_time` | Time the order was cancelled. |
| `refund_time` | Time the order was marked refunded. |
| `chargeback_time` | Time the order was marked chargeback. |

Existing `idempotency_key` remains the client request idempotency key, not the provider callback key.

### Payment Event

Create `gl_purchase_payment_event`:

| Field | Purpose |
| --- | --- |
| `id` | Primary key. |
| `tenant_id` | Tenant id. |
| `event_key` | Unique callback/event idempotency key. |
| `purchase_order_no` | Internal purchase order number. |
| `provider_code` | Provider code. |
| `provider_order_no` | Provider order id. |
| `event_type` | `PAY_SUCCESS`, `PAY_FAILED`, `CANCELLED`, `REFUNDED`, `CHARGEBACK`. |
| `event_status` | `RECEIVED`, `PROCESSED`, `IGNORED`, `FAILED`. |
| `request_hash` | Hash of the normalized event payload. |
| `request_body` | Raw or normalized event payload JSON. |
| `process_result` | Short result or failure reason. |
| `process_time` | Processing time. |
| `create_time` | Event creation time. |

Unique key: `(tenant_id, event_key)`.

## Event Semantics

### `PAY_SUCCESS`

- If event key already exists with the same request hash, return the current order state without changing wallet again.
- If event key exists with a different request hash, reject as idempotency conflict.
- Lock the purchase order by `purchase_order_no`.
- If order is already `CREDITED`, store the event as `IGNORED` and return the order.
- If order is not `CREATED`, `PENDING`, or `PAID`, reject with a business error.
- Mark order `PAID` and set `paid_time` if not already paid.
- Snapshot grants and credit wallet using the existing `purchase:credit:{orderNo}:{currency}:{grantType}` idempotency keys.
- Mark order `CREDITED`, set `credited_time`, persist `callback_event_key`.

### `PAY_FAILED`

- Only `CREATED` or `PENDING` orders may become `FAILED`.
- Store `fail_reason`.
- No wallet credit is executed.

### `CANCELLED`

- Only `CREATED` or `PENDING` orders may become `CANCELLED`.
- Set `cancel_time`.
- No wallet credit is executed.

### `REFUNDED` and `CHARGEBACK`

For this phase, these are record-only terminal markers:

- `REFUNDED` can be applied only after `PAID` or `CREDITED`.
- `CHARGEBACK` can be applied only after `PAID` or `CREDITED`.
- Set `refund_time` or `chargeback_time`.
- Do not reverse wallet balances in this phase.
- Record `process_result` explaining that wallet reversal is deferred to a later phase.

## Service Shape

Add:

- `PurchaseOrderStatus`
- `PurchasePaymentEventType`
- `PurchasePaymentEventStatus`
- `PurchasePaymentEvent`
- `PurchasePaymentCallbackBo`
- `PurchasePaymentEventMapper`
- `IPurchasePaymentEventService`
- `PurchasePaymentEventServiceImpl`

The service method:

```java
PurchaseOrder applyEvent(PurchasePaymentCallbackBo bo);
```

`ClientPurchaseService.pay(...)` should still satisfy the H5 demo by creating a simulated order and immediately calling:

```java
purchasePaymentEventService.applyEvent(PurchasePaymentCallbackBo.simulatedSuccess(order));
```

This keeps the external C-side response compatible while proving the future callback path.

## Admin And UI

No new Admin page in this phase.

Existing purchase offer Admin UI remains unchanged. Purchase order visibility can be added later as a separate B-side order query page once the backend event model stabilizes.

## SQL

Update `backend/script/sql/gameluck_wallet.sql` idempotently:

- Add new purchase order columns if missing.
- Add indexes:
  - `(tenant_id, provider_code, provider_order_no)`
  - `(tenant_id, payment_session_no)`
- Create `gl_purchase_payment_event` if missing.

Do not drop or rewrite existing rows.

## Testing

Focused tests:

- `PurchasePaymentEventServiceImplTest`
  - `paySuccessCreditsWalletOnceWhenEventRepeated`
  - `sameEventKeyWithDifferentPayloadIsRejected`
  - `payFailedMarksPendingOrderFailedWithoutWalletCredit`
  - `cancelledMarksPendingOrderCancelledWithoutWalletCredit`
  - `refundAndChargebackAreRecordOnlyForCreditedOrders`
- `ClientPurchaseServiceTest`
  - existing simulated purchase still returns `CREDITED`
  - existing purchase idempotency behavior still returns existing order without new event processing
  - order creation sets provider/session fields before simulated event processing

Build checks:

- Focused Maven tests for payment module.
- Backend package.
- Runtime smoke:
  - H5 register or use existing token.
  - Buy an offer.
  - Confirm purchase order status is `CREDITED`.
  - Confirm exactly one `gl_purchase_payment_event` row.
  - Repeat the same C-side idempotency key and confirm order/event counts do not increase.

## Acceptance Criteria

- Purchase order has explicit provider/session/event fields.
- Payment event table exists and is idempotent by `(tenant_id, event_key)`.
- Simulated purchase uses the same event application service that future real callbacks will use.
- Repeated payment success events do not double-credit wallets.
- Conflicting repeated event keys are rejected.
- Failed/cancelled events do not credit wallets.
- Refunded/chargeback events are recorded without wallet reversal.
- Existing C-side purchase API remains compatible for the demo path.
- Focused payment tests and backend package pass.

## Self Review

- Scope is focused on backend readiness and avoids real provider integration.
- No placeholder requirements are left.
- Refund and chargeback wallet reversal are intentionally excluded and documented.
- The design preserves current H5 behavior while improving backend boundaries.
