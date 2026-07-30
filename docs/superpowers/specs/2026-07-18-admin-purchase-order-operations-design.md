# Admin Purchase Order Operations Design

## Goal

Give B-side operators a purchase order workspace that can query C-side purchase orders, inspect payment callback/event history, and manually mark abnormal payment outcomes with full audit visibility.

This phase extends the Phase 38 payment-event foundation into an operator-facing workflow. It does not connect a real payment provider and does not reverse wallet balances for refunds or chargebacks.

## Scope

This phase includes:

- Admin backend list/detail APIs for `gl_purchase_order`.
- Admin backend event list visibility for `gl_purchase_payment_event`.
- Manual payment outcome actions:
  - Mark `CREATED` or `PENDING` orders as `PAY_FAILED`.
  - Mark `CREATED` or `PENDING` orders as `CANCELLED`.
  - Mark `PAID` or `CREDITED` orders as `REFUNDED`.
  - Mark `PAID` or `CREDITED` orders as `CHARGEBACK`.
- Each manual action creates a purchase payment event with provider code `MANUAL_ADMIN`, a deterministic event key, request body containing the operator reason, and normal operation log coverage through `@Log`.
- Admin UI page under Payment Management for purchase orders.
- Detail drawer/dialog showing order fields, grant snapshots, and payment event timeline.
- SQL menu and permission rows for the new Admin page and actions.
- Focused backend tests, Admin build/type verification where available, backend package, SQL import, and runtime smoke.

This phase excludes:

- Real provider SDKs.
- Webhook signature verification.
- Wallet refund reversal.
- Chargeback wallet clawback.
- Manual wallet credit or debit.
- C-side UI changes.
- A separate provider reconciliation job.

## Current State

Phase 38 added:

- Provider/session/callback fields to `gl_purchase_order`.
- `gl_purchase_payment_event`.
- `PurchasePaymentEventServiceImpl.applyEvent(...)`.
- Simulated C-side purchase that applies a simulated `PAY_SUCCESS` event and returns `CREDITED`.

The missing B-side capability is operational visibility. Operators can configure purchase offers and deposit orders, but they cannot inspect purchase orders, callback events, or mark payment-provider abnormalities.

## Backend Design

### API Shape

Add `PurchaseOrderController`:

- `GET /payment/purchase-order/list`
  - Permission: `payment:purchaseOrder:list`
  - Returns paged `PurchaseOrderVo` rows.
- `GET /payment/purchase-order/{id}`
  - Permission: `payment:purchaseOrder:query`
  - Returns one `PurchaseOrderDetailVo`.
- `POST /payment/purchase-order/{id}/mark-failed`
  - Permission: `payment:purchaseOrder:manual`
  - Body: `{ "reason": "..." }`
- `POST /payment/purchase-order/{id}/cancel`
  - Permission: `payment:purchaseOrder:manual`
  - Body: `{ "reason": "..." }`
- `POST /payment/purchase-order/{id}/refund`
  - Permission: `payment:purchaseOrder:manual`
  - Body: `{ "reason": "..." }`
- `POST /payment/purchase-order/{id}/chargeback`
  - Permission: `payment:purchaseOrder:manual`
  - Body: `{ "reason": "..." }`

All manual endpoints return the updated `PurchaseOrderDetailVo`.

### Query Model

Add `PurchaseOrderBo` filters:

- `purchaseOrderNo`
- `memberId`
- `memberNo`
- `offerId`
- `offerNo`
- `status`
- `providerCode`
- `providerOrderNo`
- `paymentSessionNo`
- `idempotencyKey`
- `beginTime`
- `endTime`

List ordering is `create_time desc`.

### Detail Model

`PurchaseOrderDetailVo` includes:

- Order fields from `PurchaseOrderVo`.
- `grantSnapshots`: rows from `gl_purchase_order_grant_snapshot`.
- `paymentEvents`: rows from `gl_purchase_payment_event` ordered by `create_time asc`.

The detail view is read-only except for manual action buttons that remain available when the current status allows the action.

### Manual Action Rules

Manual actions call the existing event service instead of mutating order rows directly.

| Admin action | Event type | Allowed source statuses | Wallet effect |
| --- | --- | --- | --- |
| Mark failed | `PAY_FAILED` | `CREATED`, `PENDING` | None |
| Cancel | `CANCELLED` | `CREATED`, `PENDING` | None |
| Refund | `REFUNDED` | `PAID`, `CREDITED` | None in this phase |
| Chargeback | `CHARGEBACK` | `PAID`, `CREDITED` | None in this phase |

Reason is required for every manual action, max 500 characters after trimming.

Manual event key format:

```text
purchase:manual:{action}:{purchaseOrderNo}:{yyyyMMddHHmmssSSS}
```

Manual event request body format:

```json
{
  "source": "ADMIN",
  "action": "REFUNDED",
  "reason": "Provider reported refund completed"
}
```

Provider fields:

- `providerCode = MANUAL_ADMIN`
- `providerOrderNo = existing providerOrderNo if present, otherwise purchaseOrderNo`

### Audit And Error Handling

- Controller manual endpoints use `@Log(title = "...", businessType = BusinessType.UPDATE)`.
- Invalid order id returns `payment.purchase.order.not.exists`.
- Invalid status reuses `payment.purchase.order.status.invalid`.
- Missing reason returns new key `payment.purchase.manual.reason.required`.
- Unsupported action is not accepted at controller/service method level.
- Detail query returns empty `grantSnapshots` or `paymentEvents` arrays when none exist.

## Admin UI Design

Target: B-side operational page.

Route:

- `admin-ui/src/views/payment/purchase-order/index.vue`

Page structure:

- Search form:
  - purchase order no
  - member id/member no normalized with existing helper
  - status
  - provider code
  - provider order no
  - payment session no
  - idempotency key
- Status segmented control:
  - All
  - Pending
  - Credited
  - Failed
  - Cancelled
  - Refunded
  - Chargeback
- Dense table:
  - purchase order no
  - member
  - offer
  - pay currency / amount
  - status tag
  - provider code
  - provider order no
  - payment session no
  - callback event key
  - paid time
  - credited time
  - created time
- Row actions:
  - detail
  - mark failed for `CREATED/PENDING`
  - cancel for `CREATED/PENDING`
  - refund for `PAID/CREDITED`
  - chargeback for `PAID/CREDITED`

Detail dialog:

- Order metadata via descriptions.
- Grant snapshots table.
- Payment event timeline/table with event key, type, status, provider refs, process result, process time, created time.

Manual action dialog:

- Shows order no, current status, target action.
- Requires reason.
- Requires confirmation before submit.
- Shows loading state while submitting.

UI states:

- Loading table state.
- Empty table state via Element Plus default empty slot.
- Filtered empty state through existing table behavior.
- Permission-hidden action buttons through `v-hasPermi`.
- Manual action validation failure if reason is blank.
- Network/API failure handled by existing request/modal behavior.

## SQL

Update `backend/script/sql/gameluck_platform_dict.sql`:

- Add purchase order status dictionary `gl_purchase_order_status`.
- Add purchase payment event status dictionary `gl_purchase_payment_event_status`.
- Add purchase payment event type dictionary `gl_purchase_payment_event_type`.

Update menu SQL:

- Add `Payment Management -> Purchase Orders` page if not present.
- Add permissions:
  - `payment:purchaseOrder:list`
  - `payment:purchaseOrder:query`
  - `payment:purchaseOrder:manual`

SQL must be idempotent and safe to import repeatedly.

## Testing

Backend tests:

- Query wrapper supports order no, member id/member no, status, provider fields, idempotency key, and date range.
- Detail loads order, grant snapshots, and events.
- Manual failed/cancel/refund/chargeback actions require reason.
- Manual actions call `IPurchasePaymentEventService.applyEvent(...)` with the expected event type and provider fields.
- Invalid source status is rejected by the event service; admin service does not bypass it.

Verification:

- Focused Maven tests for payment module.
- SQL import into local MySQL.
- Backend package.
- Runtime smoke:
  - Use the Phase 38 purchase order or create a fresh purchase.
  - Admin API list finds the order.
  - Admin API detail returns event history.
  - Create a DB-seeded `PENDING` purchase order and manually cancel it through Admin API.
  - Confirm order status becomes `CANCELLED`.
  - Confirm one manual payment event row exists.
  - Confirm operation log captures the manual endpoint.
- Admin UI build/type check where the current workspace allows it.
- Browser/UI smoke if the Admin dev server is available.
- `git diff --check`.

## Acceptance Criteria

- Operators can list and filter C-side purchase orders.
- Operators can inspect provider/session/idempotency fields.
- Operators can inspect grant snapshots and payment events for a purchase order.
- Operators can manually mark failed, cancelled, refunded, or chargeback outcomes only from allowed source statuses.
- Manual actions require a reason and create a payment event.
- Manual actions do not directly modify wallet balances.
- Manual actions are covered by Admin operation logs.
- Existing C-side simulated purchase behavior remains compatible.

## Self Review

- Scope is focused on Admin visibility and controlled manual outcome marking.
- Refund and chargeback wallet reversal are explicitly deferred.
- Manual operations reuse the Phase 38 event service, so order state transitions stay centralized.
- No placeholder requirements are left.
