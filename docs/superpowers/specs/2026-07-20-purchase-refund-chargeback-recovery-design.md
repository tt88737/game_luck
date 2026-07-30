# Purchase Refund And Chargeback Recovery Design

## Goal

Turn purchase refund and chargeback events into an auditable wallet recovery workflow that reverses every granted asset when funds are available and creates an explicit manual recovery case when a full reversal is impossible.

The workflow must never create negative wallet balances or silently perform a partial recovery. Chargebacks also raise the member risk level to `HIGH`.

## Scope

This phase includes:

- Recover all assets represented by purchase grant snapshots for `REFUNDED` and `CHARGEBACK` events.
- Aggregate snapshot amounts by currency before wallet operations.
- Perform an atomic all-or-nothing multi-currency wallet debit.
- Cancel outstanding turnover tasks created by the purchase after a successful recovery.
- Record a durable recovery case for both completed and review-required outcomes.
- Record per-currency required, recovered, available, and shortfall amounts.
- Add purchase order review statuses for insufficient-balance outcomes.
- Raise the member risk level to `HIGH` for every processed chargeback.
- Expose recovery details in the existing Admin purchase order detail page.
- Add SQL migrations, dictionaries, localized messages, focused tests, backend/Admin builds, and runtime smoke verification.

This phase excludes:

- Negative wallet balances.
- Partial wallet recovery.
- An operator action for collecting or writing off the remaining debt.
- Reopening completed turnover tasks.
- Returning recovered assets to a payment provider.
- Real provider SDK, signature verification, or reconciliation jobs.
- Changing C-side purchase UI.

## Current State

The payment event service already accepts `REFUNDED` and `CHARGEBACK`, but it only updates the purchase order status and writes a deferred-wallet message. Purchase grant snapshots identify every currency and amount credited for an order.

The wallet core supports idempotent single-currency debit and rejects insufficient available balance. It does not currently expose an atomic multi-currency debit. Turnover tasks support `PENDING` and `COMPLETED`, but no cancellation operation.

The existing event service catches runtime failures inside one transaction. A thrown recovery exception would roll back the event and any recovery record, so insufficient funds must be represented as a normal review outcome rather than an exception.

## Domain Model

### Purchase Order Status

Add:

- `REFUND_REVIEW`
- `CHARGEBACK_REVIEW`

Final meanings:

| Event | Recovery result | Order status |
| --- | --- | --- |
| `REFUNDED` | full recovery completed | `REFUNDED` |
| `REFUNDED` | insufficient balance | `REFUND_REVIEW` |
| `CHARGEBACK` | full recovery completed | `CHARGEBACK` |
| `CHARGEBACK` | insufficient balance | `CHARGEBACK_REVIEW` |

Review statuses are terminal for automated event replay in this phase. Manual resolution is deferred.

### Recovery Case

Add `gl_purchase_reversal`:

| Field | Purpose |
| --- | --- |
| `id` | Snowflake primary key |
| `tenant_id` | tenant isolation |
| `reversal_no` | public recovery number |
| `purchase_order_id` | internal order id |
| `purchase_order_no` | purchase business number |
| `member_id` | affected member |
| `event_key` | source payment event key |
| `reversal_type` | `REFUND` or `CHARGEBACK` |
| `status` | `PROCESSING`, `COMPLETED`, `REVIEW_REQUIRED` |
| `reason` | provider/admin reason copied from the callback |
| `review_reason` | normalized insufficient-balance summary |
| `completed_time` | full recovery completion time |
| `create_time`, `update_time` | audit timestamps |

Unique keys:

- `(tenant_id, reversal_no)`
- `(tenant_id, event_key)`

The event key is the idempotency boundary. Replaying the same event returns the existing recovery result without another debit or risk update.

### Recovery Items

Add `gl_purchase_reversal_item`:

| Field | Purpose |
| --- | --- |
| `id` | Snowflake primary key |
| `tenant_id` | tenant isolation |
| `reversal_id`, `reversal_no` | parent recovery |
| `purchase_order_no` | source order |
| `member_id` | affected member |
| `currency_code` | aggregated grant currency |
| `required_amount` | amount that must be recovered |
| `available_amount` | locked balance observed during preflight |
| `recovered_amount` | full amount on success, zero on review |
| `shortfall_amount` | `max(required - available, 0)` |
| `wallet_transaction_no` | successful recovery debit transaction |
| `status` | `COMPLETED` or `REVIEW_REQUIRED` |
| `create_time`, `update_time` | audit timestamps |

Unique key: `(tenant_id, reversal_no, currency_code)`.

Snapshot rows are aggregated by uppercase currency code. Non-positive totals are rejected as invalid recovery data.

## Wallet Boundary

### Atomic Batch Debit

Add a wallet-owned batch debit API rather than exposing wallet account mappers to payment.

Input:

- tenant id;
- member id;
- business number and source type `PURCHASE_REVERSAL`;
- one positive debit line per currency;
- deterministic idempotency key per currency.

Behavior:

1. Normalize and sort lines by currency code to establish deterministic lock order.
2. Lock every wallet account with `selectByBizKeyForUpdate`.
3. Capture available balances for every line.
4. If any account is missing or insufficient, return `REVIEW_REQUIRED` with all observed balances and shortfalls. Do not create wallet transactions and do not change any balance.
5. If every line is sufficient, execute the existing idempotent debit behavior for every line in the same transaction.
6. Return `COMPLETED` with transaction numbers.

The batch service participates in the caller transaction. Unexpected technical errors still throw and roll back the payment event, recovery rows, wallet changes, turnover changes, and risk update together.

### Turnover Cancellation

Add `CANCELLED` as a turnover task status and a wallet service method that cancels `PENDING` tasks by tenant, member, and purchase business number.

Rules:

- Run only after full wallet recovery succeeds.
- Change only `PENDING` tasks to `CANCELLED`.
- Preserve `COMPLETED` tasks for audit history.
- Set update time and remark referencing the recovery number.
- Repeated cancellation is idempotent and returns zero changed rows.

## Payment Recovery Workflow

Add `IPurchaseReversalService` in the payment module. `PurchasePaymentEventServiceImpl` delegates `REFUNDED` and `CHARGEBACK` events to it instead of directly setting a final status.

Workflow:

1. Lock and validate the purchase order in `PAID` or `CREDITED`.
2. Resolve an existing reversal by event key; return it if present.
3. Load purchase grant snapshots by order number.
4. Require at least one snapshot and aggregate amounts by currency.
5. Insert the `PROCESSING` recovery case and item rows.
6. Call the wallet atomic batch debit.
7. On `COMPLETED`:
   - update every item with recovered amount and transaction number;
   - cancel pending purchase turnover tasks;
   - mark recovery `COMPLETED`;
   - set order to `REFUNDED` or `CHARGEBACK`;
   - populate the matching event time and clear deferred failure text.
8. On `REVIEW_REQUIRED`:
   - update every item with available and shortfall amounts;
   - leave wallet balances and turnover tasks unchanged;
   - mark recovery `REVIEW_REQUIRED`;
   - set order to `REFUND_REVIEW` or `CHARGEBACK_REVIEW`;
   - store a localized review reason on the order and recovery case.
9. For every `CHARGEBACK` outcome, update the member to `risk_level='HIGH'` and record the source event/recovery number in dedicated risk audit fields.
10. Return the updated order. The payment event becomes `PROCESSED`; `process_result` is `OK` or `REVIEW_REQUIRED`.

An event for an order already in the corresponding final/review status is accepted only through the same event-key idempotency path. A different event key is rejected by the existing order-status validation to prevent duplicate recoveries.

## Chargeback Risk Update

Chargeback risk escalation is mandatory whether wallet recovery completes or requires review.

Rules:

- Lock or version-check the member row before update.
- Set `risk_level` to `HIGH`.
- Add `risk_reason`, `risk_source`, and `risk_updated_time` to `gl_member_profile` and the member domain/VO.
- Set `risk_reason` to a localized chargeback description.
- Set `risk_source` to `PURCHASE_CHARGEBACK:{reversalNo}:{eventKey}`.
- Set `risk_updated_time` to the recovery processing time.
- Do not downgrade or overwrite other compliance fields.
- Replaying the same event must not produce an additional logical risk change.

The existing purchase and redemption compliance gates will immediately block subsequent new requests after the transaction commits.
Admin member detail must display the three risk audit fields so operators can trace why the level changed.

## Admin Experience

Extend the existing purchase order detail response with:

- recovery number;
- recovery type and status;
- reason and review reason;
- completion time;
- item rows with currency, required, available, recovered, shortfall, status, and wallet transaction number.

The Admin purchase order page adds a read-only “资产追偿” section below grant snapshots. Use status tags and a dense table. `REVIEW_REQUIRED` must visibly show the shortfall by currency.

Manual refund and chargeback actions continue to require a reason and confirmation. Their warning copy changes from “only records status” to state that the action attempts a full asset recovery and may create a manual recovery case.

No new operator mutation is added for recovery cases in this phase.

## Error Handling And Audit

Business review outcomes do not throw:

- missing wallet account;
- insufficient available balance.

They produce `REVIEW_REQUIRED` records and a processed payment event.

Technical or integrity failures throw and roll back:

- missing purchase order;
- missing grant snapshots;
- non-positive aggregated amount;
- duplicate event with conflicting request hash;
- wallet transaction idempotency conflict;
- database or concurrency failure.

Admin manual endpoints retain operation logs. Payment events, recovery cases, recovery items, wallet transactions, turnover task remarks, and member risk updates provide the business audit chain.

## Testing

### Wallet Tests

- Multi-currency batch with sufficient balances debits every currency atomically.
- One insufficient currency returns `REVIEW_REQUIRED` and changes no balances or transactions.
- Missing account produces a review item with full shortfall.
- Deterministic idempotent replay returns original transaction numbers.
- Pending turnover tasks are cancelled; completed tasks remain unchanged.

### Payment Tests

- Refund success reverses every aggregated snapshot and sets `REFUNDED`.
- Refund insufficiency creates review rows, leaves wallet/turnover unchanged, and sets `REFUND_REVIEW`.
- Chargeback success sets `CHARGEBACK` and raises member risk to `HIGH`.
- Chargeback insufficiency sets `CHARGEBACK_REVIEW` and still raises risk to `HIGH`.
- Same event replay does not debit twice or update risk twice.
- Different event key against a final/review order is rejected.
- Missing snapshots and invalid amounts roll back the event and recovery rows.

### Admin Tests And Runtime Smoke

- Backend detail API returns recovery case and items.
- Admin UI displays completed and review-required recovery outcomes without overflow.
- Runtime refund with sufficient balances produces debit transactions and cancelled pending turnover tasks.
- Runtime chargeback with an intentionally insufficient currency produces no debit, records exact shortfall, and changes member risk to `HIGH`.
- Replaying both event keys leaves counts and balances unchanged.

## Acceptance Criteria

- Refund and chargeback events attempt full recovery of every purchase grant snapshot.
- Recovery is all-or-nothing across currencies and never creates a negative balance.
- Insufficient funds create durable review cases with per-currency shortfalls and no partial debit.
- Successful recovery cancels only pending purchase turnover tasks.
- Chargebacks always set member risk to `HIGH`.
- Event replay is idempotent; conflicting or second independent reversal attempts are rejected.
- Admin order detail shows the full recovery audit trail.
- SQL imports are idempotent.
- Focused wallet/payment/member tests, Admin checks, backend package, runtime smoke, and `git diff --check` pass.
