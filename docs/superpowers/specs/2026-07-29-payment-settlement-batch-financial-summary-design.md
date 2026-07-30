# Payment Settlement Batch And Financial Summary Design

## 1. Goal

Phase 45 adds a tenant-scoped payment settlement workbench that converts processed Provider payment events into immutable financial snapshots. Operators can calculate gross collections, refunds, chargebacks, simulated fees, and net settlement by Provider, payment currency, and UTC settlement window, then close the batch only after the related Phase 44 reconciliation evidence is complete.

This phase does not send money, connect to a bank, create invoices or accounting journals, convert currencies, or integrate a real payment Provider. It never changes payment sessions, purchase orders, webhook events, payment events, reversals, member risk, turnover tasks, or wallets.

## 2. Product Boundary

The workbench answers four operational questions:

1. Which processed Provider events belong to this settlement window?
2. What are gross payment, refund, chargeback, fee, and net amounts in each payment currency?
3. Which reconciliation batches and open discrepancies block financial close?
4. Who calculated or closed the batch, with which fee rule and remark?

One batch covers exactly one tenant, Provider, payment currency, and half-open UTC interval `[periodStart, periodEnd)`. Cross-currency netting is forbidden. Overlapping non-failed batches for the same tenant, Provider, and currency are rejected so one Provider event cannot be settled twice.

## 3. Source Of Truth

Only `gl_payment_webhook_event` rows with status `PROCESSED` and one of these event types are financial source events:

- `PAYMENT_SUCCEEDED`
- `REFUND_SUCCEEDED`
- `CHARGEBACK_CREATED`

`PAYMENT_FAILED` and `PAYMENT_CANCELLED` are operational events and do not affect settlement money. The settlement engine joins each webhook to the immutable payment session and purchase order identity. Amount and currency come from the session/order values already verified by `PaymentWebhookBusinessProcessor`; identity or amount disagreement fails calculation rather than being silently included.

Window membership uses `received_time` normalized as UTC. This is explicit for Phase 45 and is stored in every settlement item. A future real Provider may introduce a separate Provider settlement timestamp through a new adapter contract; Phase 45 does not infer one from raw JSON.

## 4. Financial Formula

All money uses `DECIMAL(20,6)` and `BigDecimal`. Rates use `DECIMAL(12,8)`. Every intermediate multiplication rounds to six decimals with `HALF_UP`.

The batch snapshots three non-negative simulated fee inputs:

- `payment_fee_rate`: proportional fee applied to successful payments, from `0` through `1`;
- `payment_fixed_fee`: fixed fee per successful payment in the batch currency;
- `chargeback_fixed_fee`: fixed fee per chargeback in the batch currency.

Per event:

```text
PAYMENT_SUCCEEDED:
  gross_payment = amount
  refund = 0
  chargeback = 0
  fee = round(amount * payment_fee_rate, 6) + payment_fixed_fee

REFUND_SUCCEEDED:
  gross_payment = 0
  refund = amount
  chargeback = 0
  fee = 0

CHARGEBACK_CREATED:
  gross_payment = 0
  refund = 0
  chargeback = amount
  fee = chargeback_fixed_fee
```

Batch totals:

```text
net_settlement = gross_payment - refund - chargeback - total_fee
```

A negative net settlement is valid and must be displayed as payable to the Provider; it is not clamped to zero. Refund processing fees, tax, reserves, rolling holds, FX, and partial refunds are outside Phase 45.

## 5. Persistence

### 5.1 Settlement Batch

`gl_payment_settlement_batch` stores:

- tenant, settlement number, Provider, currency, UTC start/end;
- state and optimistic-lock version;
- the three immutable fee-rule snapshots;
- event counts and the five financial totals;
- reconciliation coverage count, open issue count, and evidence snapshot JSON;
- creator/calculator/closer identity, close remark, timestamps, and sanitized failure reason.

The tenant-first unique business key is `(tenant_id, provider_code, currency_code, period_start, period_end)`. A mapper overlap query rejects any intersecting batch except `FAILED` and the current batch.

### 5.2 Settlement Item

`gl_payment_settlement_item` is immutable after calculation and contains one row per included webhook:

- batch, webhook, session, order, and Provider event identities;
- event type and UTC received time;
- currency and source amount;
- gross payment, refund, chargeback, fee, and signed net contribution;
- a canonical source snapshot JSON with identifiers and verified platform values, excluding raw webhook bodies and secrets.

`(tenant_id, webhook_event_id)` is unique across settlement items. Calculation inserts items and totals atomically.

### 5.3 Action Log

`gl_payment_settlement_action_log` is append-only and records create, calculate, calculation failure, close rejection, and close success with operator, before/after state, sanitized remark, evidence summary, and time.

## 6. State Machine

```text
CREATED -> CALCULATING -> CALCULATED -> CLOSED
   |            |
   +------------+-> FAILED
```

- `CREATED`: identity, window, currency, and fee snapshot are persisted; no items exist.
- `CALCULATING`: one guarded caller owns calculation.
- `CALCULATED`: immutable items and totals are complete; reconciliation evidence is queryable.
- `CLOSED`: reconciliation coverage and discrepancy gates passed and a required close remark was recorded.
- `FAILED`: calculation infrastructure or source-integrity failure was recorded in a separate transaction; no partial items remain.

`CLOSED` is terminal. Fee inputs, period, Provider, and currency cannot be edited. A failed or incorrectly scoped batch is replaced by a new non-overlapping batch after the failed batch is retained for audit.

## 7. Calculation And Concurrency

Calculation uses a guarded `CREATED -> CALCULATING` update. It pages eligible webhooks in bounded chunks, loads sessions/orders in batches, validates identity and currency, creates deterministic item snapshots, and computes totals.

The item inserts, totals, and `CALCULATING -> CALCULATED` transition share one transaction. Any error rolls them back. A separate `REQUIRES_NEW` recorder transitions the owned batch to `FAILED` and stores a stable failure code. Concurrent callers receive a conflict and create no duplicate items or logs.

The source query is tenant-scoped and ordered by `received_time, id`. The engine never parses money from `raw_body`, logs it, or exposes it through settlement APIs.

## 8. Reconciliation Close Gate

Closing recalculates evidence at command time; it never trusts counts captured during calculation.

For every UTC calendar date touched by `[periodStart, periodEnd)`, there must be at least one `COMPLETED` Phase 44 reconciliation batch for the same tenant and Provider. Evidence includes every relevant completed batch ID and statement date. The close is blocked when:

- any touched date has no completed reconciliation batch;
- any relevant completed batch contains an `OPEN` issue whose Provider or platform currency equals the settlement currency;
- the settlement batch is not `CALCULATED` or its version changed;
- a source webhook already belongs to another settlement item.

`RESOLVED` and `IGNORED` issues do not block close, but their counts and batch IDs remain in the evidence snapshot. Closing stores the exact evidence snapshot, actor, nonblank remark, and time in one guarded update. It does not change reconciliation records.

## 9. Admin API And Permissions

```text
GET  /payment/settlement/list
POST /payment/settlement
GET  /payment/settlement/{batchId}
GET  /payment/settlement/{batchId}/items
POST /payment/settlement/{batchId}/calculate
POST /payment/settlement/{batchId}/close
```

Permissions:

```text
payment:settlement:list
payment:settlement:query
payment:settlement:create
payment:settlement:calculate
payment:settlement:close
```

Create, calculate, and close use Admin operation logging and the domain action log. JavaScript-sensitive identifiers are strings in frontend contracts; monetary values and rates are serialized as strings.

## 10. Admin Experience

Payment Center gains `Payment Settlement` after `Payment Reconciliation`.

The list supports Provider, currency, state, period, and settlement-number filters. It shows event counts, gross payment, refunds, chargebacks, fees, net settlement, reconciliation blockers, creator, and timestamps.

The create dialog contains Provider, currency, UTC start/end, fee rate, payment fixed fee, and chargeback fixed fee. It validates start before end, maximum window of 31 days, non-negative fee values, and rate at most one.

The detail drawer uses an unframed summary band and tabs for event items, reconciliation evidence, and action history. Calculate and close are permission-scoped. Close requires a remark and explicitly states that no order or wallet value changes. Missing date coverage and open issues link to the existing reconciliation workbench.

Desktop tables remain dense and operational. At 390 px, summary values stack, commands remain reachable, and wide item tables scroll within their own region without page-level overflow.

## 11. Errors And Security

- All reads and writes are tenant-scoped; cross-tenant identifiers look absent.
- Provider, currency, rate, time range, remark, and page inputs are server validated.
- Windows longer than 31 days, future end times, overlapping batches, unsupported Providers, and currencies without matching source events are rejected with stable localized messages.
- Raw webhook bodies, signatures, secrets, stack traces, SQL, and filesystem paths never enter settlement snapshots, logs, or responses.
- Close conflicts return a stable business conflict and never create a success action log.

## 12. Testing And Acceptance

Backend tests cover persistence constraints, formulas and rounding, negative net settlement, event eligibility, UTC boundaries, tenant/currency isolation, identity mismatch, overlap protection, calculation rollback/failure persistence, concurrent calculate/close, reconciliation date coverage, open/resolved/ignored issues, and absence of wallet dependencies.

Frontend checks cover typed API contracts, string identifiers/money, permissions, bilingual labels, form validation, terminal states, blocker links, close confirmation, and responsive layout.

Runtime acceptance creates simulated success, refund, and chargeback events in one currency; calculates exact item and batch totals; demonstrates close rejection for missing reconciliation coverage and an open issue; resolves or ignores the issue through Phase 44; closes the batch; replays close to confirm terminal protection; and verifies source payment, reversal, risk, turnover, and wallet snapshots are unchanged.

SQL imports twice without duplicate tables, menus, permissions, dictionaries, or rows. Focused and cross-module Maven tests, backend package, Admin and H5 builds, desktop and 390 px browser evidence, service HTTP checks, and `git diff --check` must pass.

## 13. Non-Goals

- Real Provider SDKs, credentials, APIs, payouts, or settlement files.
- Bank accounts, payout instructions, treasury execution, invoices, tax, or general-ledger posting.
- FX conversion or cross-currency netting.
- Partial refunds, reserves, fee tax, tiered pricing, or retroactive fee-rule edits.
- Automated scheduling, message queues, or a new payment microservice.
