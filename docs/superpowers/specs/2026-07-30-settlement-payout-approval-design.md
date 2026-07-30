# Phase 47 Settlement Payout Approval Design

## 1. Goal

Phase 47 adds an internal settlement payout-instruction and approval loop on top of immutable Phase 45 closed settlement batches and the Phase 46 reporting surface. Payment operations can create, submit, review, approve, reject, and cancel an instruction while preserving clear separation between settlement calculation and payout authorization.

An approved instruction means only that the platform's internal review is complete. It does not mean money was transferred, does not call a bank or Provider payout API, and does not update payment, reconciliation, settlement, member, turnover, or wallet source state.

## 2. Scope

Phase 47 delivers:

- one tenant-scoped payout instruction per eligible settlement batch;
- a two-person maker-checker approval workflow;
- immutable instruction amount and settlement evidence snapshots;
- editable operational purpose and payee reference fields before resubmission;
- dedicated list, detail, create, submit, approve, reject, and cancel permissions;
- an immutable action history;
- entry points from existing settlement batch and settlement-report drill-down views;
- a dense Admin operations workbench with responsive detail and action states;
- idempotent SQL metadata, focused tests, builds, and runtime acceptance.

Only `CLOSED` settlement batches with `net_settlement > 0` are eligible. Zero-net batches require no payout instruction. Negative-net batches represent an exception or receivable condition and remain visible in settlement reporting, but Phase 47 does not create a reverse collection workflow.

## 3. Approaches Considered

### 3.1 Independent Payout Instruction Aggregate (Selected)

Create dedicated payout-instruction and action-log tables and services. A unique tenant/batch relationship provides traceability without changing Phase 45 terminal settlement semantics. The aggregate owns its workflow and can later become an input to a separately designed execution integration.

### 3.2 Extend Settlement Batch State

Adding payout fields and states directly to settlement batches would reduce the file count, but it would couple financial calculation closure to payout approval and weaken the meaning of the existing `CLOSED` terminal state.

### 3.3 General Approval Engine

A reusable approval engine could support other workflows, but it would introduce generic task routing, subject polymorphism, and broader permissions before there is a second proven use case. Phase 47 keeps the workflow domain-specific.

## 4. Domain Model

One payout instruction belongs to exactly one tenant and one Phase 45 settlement batch. A batch can have at most one instruction, including after rejection or cancellation. Rejected instructions are edited and resubmitted in place rather than replaced with another record.

The instruction stores:

- string-safe instruction ID and instruction number;
- tenant ID and settlement batch ID/number;
- Provider code and ISO currency code copied from the batch;
- immutable six-decimal payout amount copied from positive `net_settlement`;
- immutable settlement evidence summary containing only non-sensitive batch facts;
- operational payout purpose and payee reference text;
- current status and optimistic-lock version;
- maker, submitter, latest reviewer, and timestamps;
- rejection, cancellation, or approval reason where applicable.

The payee reference is an operator-facing label such as a merchant account alias or internal beneficiary code. Phase 47 must not store bank-account numbers, routing numbers, card data, credentials, secrets, signed payout payloads, or payment tokens.

All JavaScript-sensitive identifiers and monetary values are serialized as strings. The payout amount never comes from a client request and cannot be edited after creation.

## 5. State Machine

```text
DRAFT -> PENDING_APPROVAL -> APPROVED
  ^              |
  |              +-------> REJECTED
  |                          |
  +--------------------------+
  |
  +-----------------------> CANCELLED
```

Rules:

- creation produces `DRAFT`;
- only `DRAFT` can be edited, submitted, or cancelled;
- `REJECTED` can be edited and then resubmitted;
- editing a rejected instruction returns it to `DRAFT`, retaining the rejection in action history;
- only `PENDING_APPROVAL` can be approved or rejected;
- `APPROVED` and `CANCELLED` are terminal;
- the reviewer must have an approve permission and must not be the maker;
- every command requires the client's expected version and uses a guarded conditional update;
- stale versions, duplicate submissions, repeated terminal commands, and illegal transitions fail without partial writes.

`REJECTED` is not directly approvable. The maker must revise the operational fields and submit again so the next approval is tied to a new immutable action-history entry.

## 6. Persistence And Integrity

Add `gl_payment_settlement_payout` with tenant-first indexes and unique keys for instruction number and settlement batch. Add `gl_payment_settlement_payout_action_log` for append-only workflow evidence.

Creation runs in one transaction:

1. resolve the current tenant and operator;
2. load the tenant-visible settlement batch;
3. require `CLOSED` and positive net settlement;
4. reject an existing instruction for that tenant/batch;
5. copy Provider, currency, amount, batch number, and sanitized evidence;
6. insert the `DRAFT` instruction and `CREATE` action.

State commands use explicit mapper updates with tenant, ID, expected state, and expected version predicates. They do not use generic update-by-ID methods. A command inserts its action log only when the state update succeeds in the same transaction.

The settlement batch remains immutable. Payout queries do not lock or update payment sessions, webhook events, purchase orders, payment events, reversals, reconciliation batches/issues, settlement batches/items, member profiles, risk data, turnover tasks, wallet accounts, or wallet transactions.

## 7. Backend API

```text
GET  /payment/settlement-payout/list
GET  /payment/settlement-payout/{id}
POST /payment/settlement-payout
PUT  /payment/settlement-payout/{id}
POST /payment/settlement-payout/{id}/submit
POST /payment/settlement-payout/{id}/approve
POST /payment/settlement-payout/{id}/reject
POST /payment/settlement-payout/{id}/cancel
```

The list supports instruction number, settlement number, status, Provider, currency, and bounded creation-date filters. Results are ordered by creation time and ID descending. Detail returns the instruction plus ordered action history.

Create accepts only:

```text
settlementBatchId, payoutPurpose, payeeReference
```

Edit accepts `payoutPurpose`, `payeeReference`, and `version`. Submit accepts `version` and a required submission remark. Approve, reject, and cancel accept `version` and a required reason. Text fields are trimmed, length-bounded, and control-character guarded.

Cross-tenant or unknown instructions and batches are treated as absent. Stable localized errors cover ineligible settlement status/amount, duplicate instruction, invalid state, maker-checker conflict, stale version, missing entity, and invalid text.

## 8. Permissions And Audit

Permissions are:

```text
payment:settlementPayout:list
payment:settlementPayout:query
payment:settlementPayout:create
payment:settlementPayout:submit
payment:settlementPayout:approve
payment:settlementPayout:cancel
```

The approve permission covers both approve and reject because both are reviewer decisions. Backend authorization is authoritative. The Admin UI also hides or disables commands according to permission and state.

Create, edit, submit, approve, reject, and cancel use sanitized Admin operation logging. Request/response bodies containing purpose, payee reference, evidence snapshots, or reasons are not copied into generic operation logs. The domain action log remains the authoritative workflow audit record.

Each action records action type, before/after status, operator ID/name, reason, expected/resulting version, and a sanitized evidence summary. It never records credentials, bank details, raw Provider payloads, signatures, webhook bodies, or member data.

## 9. Admin Experience

Payment Center gains `Settlement Payout Approval` after `Settlement Report`. It is an operational workbench, not a treasury dashboard and not a payment-execution screen.

The filter band contains instruction number, settlement number, status, Provider, currency, and creation-date range. The primary table shows instruction number, settlement batch, Provider, currency, payout amount, status, maker, latest reviewer, and update time.

The detail drawer contains:

- immutable settlement batch, Provider, currency, and amount facts;
- a clear notice that approval does not transfer funds;
- payout purpose and payee reference;
- maker, submitter, reviewer, versions, and timestamps;
- a complete action timeline;
- state- and permission-scoped commands.

Creation is available from an eligible Phase 45 settlement detail and from the Phase 46 source-batch drawer. The server remains authoritative if the visible settlement state becomes stale.

Submit and cancel require confirmation. Approve and reject require a reason, show the immutable amount/currency, and repeat the no-transfer warning. Success refreshes detail, table state, and history. Version conflicts show a reload action rather than silently retrying.

Loading, empty, filtered-empty, network-error, permission-denied, command-processing, command-failed, approved, rejected, and cancelled states are explicit. At 390 px, filters stack, the wide table scrolls within its region, and the detail drawer uses full width without page-level horizontal overflow.

## 10. Error Handling And Concurrency

Validation occurs before persistence, while guarded state/version updates provide the final concurrency boundary. A unique tenant/batch key protects duplicate creation races. Duplicate-key errors are translated into the stable duplicate-instruction business error.

Command transactions either update one instruction and append one action or change nothing. A zero-row guarded update triggers a reload to distinguish missing tenant-visible data, stale version, and invalid current state. No automatic retry is used for approval commands.

Responses do not expose SQL, stack traces, internal paths, source snapshots, or secrets. Failed commands can add sanitized failure metadata to the generic operation log but do not append a successful domain action.

## 11. Testing And Runtime Acceptance

Backend tests cover:

- persistence precision, tenant-first uniqueness, and string-safe contracts;
- creation from a positive `CLOSED` batch;
- rejection of zero/negative net, non-closed, missing, and cross-tenant batches;
- immutable server-owned Provider, currency, amount, and evidence;
- duplicate and concurrent creation;
- every valid and invalid state transition;
- rejected edit and resubmission;
- maker-checker separation;
- optimistic version conflicts and terminal replay;
- atomic state/action writes and ordered history;
- permission annotations, sanitized logging, and absence of wallet/payment commands.

Frontend checks cover typed identifiers/money, menu permissions, state-scoped commands, entry-point links, bilingual copy, the no-transfer warning, complete loading/error/empty states, and responsive containment.

Runtime acceptance uses deterministic positive, zero, and negative closed batches and two Admin users. It verifies maker creation/submission, self-approval denial, second-user approval, rejection/edit/resubmission, duplicate prevention, tenant isolation, exact action history, and terminal replay. Before/after dumps prove payment, reconciliation, settlement, member, turnover, and wallet source tables are byte-identical except for the two new payout tables and sanitized Admin operation logs.

SQL imports twice without duplicate tables, menus, permissions, or dictionary rows. Focused and cross-module tests, backend package, Admin/H5 builds, service health checks, desktop/mobile screenshots, safety scans, and `git diff --check` must pass.

## 12. Non-Goals

- Bank, ACH, card, wallet, crypto, or Provider payout execution.
- Recording or claiming that funds were transferred.
- External payout status polling, callbacks, retries, or reconciliation.
- Bank accounts, routing data, credentials, secrets, tokens, or signed payloads.
- Reverse collection or accounts-receivable instructions for negative settlements.
- Multiple settlement batches in one instruction, split payouts, partial approval, or partial execution.
- Multi-level approval, configurable approval chains, delegation, or a generic workflow engine.
- Editing closed settlement totals, fee rules, reconciliation evidence, payment state, or wallet balances.
- Accounting journals, invoices, tax, reserves, FX conversion, or treasury cash forecasting.
