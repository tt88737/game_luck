# Phase 47 Payment Settlement Instruction And Review Design

## Purpose

Phase 47 turns each immutable Phase 45 closed settlement batch into one auditable operational instruction. Positive settlement values enter a manual payable review flow, negative values become receivable records, and zero values become no-action records. The system records preparation, review, and externally completed outcomes without calling a bank, moving wallet funds, or changing settlement truth.

## Scope

Phase 47 delivers:

- one tenant-scoped instruction for each eligible `CLOSED` settlement batch;
- unified `PAYABLE`, `RECEIVABLE`, and `BALANCED` directions;
- a single-reviewer payable workflow with creator/reviewer separation;
- manual external execution and collection evidence;
- receivable collection and separately authorized waiver outcomes;
- immutable source snapshots, optimistic locking, request idempotency, and append-only action logs;
- a permission-scoped Admin workbench under Payment Center;
- drill-down to the source Phase 45 batch and visibility from the Phase 46 report flow.

Phase 47 does not execute payments, connect to banks, store full bank-account data, alter wallets, or post accounting-ledger entries.

## Source And Direction Rules

An instruction is generated from exactly one tenant-visible Phase 45 settlement batch whose status is `CLOSED`. A settlement batch can generate at most one instruction. Instructions never combine batches, Providers, or currencies.

The instruction snapshots the settlement batch ID and number, Provider, currency, UTC period, close time, signed net settlement, absolute instruction amount, and the source version required for audit. Closed settlement values remain the financial truth and are never recalculated by Phase 47.

Direction is deterministic:

- `net_settlement > 0`: `PAYABLE`, with `amount = net_settlement`;
- `net_settlement < 0`: `RECEIVABLE`, with `amount = abs(net_settlement)`;
- `net_settlement = 0`: `BALANCED`, immediately terminal as `NO_ACTION`.

Every eligible closed batch therefore has a complete downstream audit link, including zero and negative values.

## Data Model

### Settlement Instruction

Add `gl_payment_settlement_instruction` with:

- tenant ID, instruction ID, and stable instruction number;
- source settlement batch ID/number and source version;
- Provider, currency, UTC period, source close time, signed net settlement, and absolute amount snapshots;
- direction and status;
- beneficiary/counterparty display name, external method, and masked destination reference;
- creator, submitter, reviewer, executor/collector/waiver operator identities and UTC timestamps where applicable;
- rejection, cancellation, execution, collection, and waiver remarks/references where applicable;
- optimistic-lock version and standard create/update timestamps.

The unique key `(tenant_id, settlement_batch_id)` enforces one instruction per source batch. IDs and money remain string-safe at API boundaries. Monetary database values use the same exact decimal scale as Phase 45.

The instruction must not store full account numbers, bank credentials, payment keys, raw bank messages, webhook payloads, or files. Destination references are already masked before persistence.

### Action Log

Add `gl_payment_settlement_instruction_action_log` as an append-only audit table containing:

- tenant, instruction, action ID, and unique client request key;
- action type, actor ID/name, before/after direction, status, and version;
- sanitized structured evidence such as masked external reference and bounded reason;
- UTC action time.

The unique key `(tenant_id, instruction_id, request_key)` makes command replay deterministic. Action logs are never updated or deleted by business services.

## State Machines

### Payable

```text
DRAFT -> PENDING_REVIEW -> APPROVED -> EXECUTED
  ^           |               |
  |           v               +-> CANCELLED
  +-------- REJECTED ---------+
```

- `DRAFT` and `REJECTED` are editable and can be submitted.
- `PENDING_REVIEW` can be approved or rejected by one reviewer.
- The reviewer must differ from the instruction creator.
- Rejection requires a reason. The creator may revise the same instruction and resubmit; the instruction number remains stable and every version is audited.
- `APPROVED` can be marked `EXECUTED` only with an external payment reference, execution time, and remark.
- Eligible nonterminal payable instructions can be cancelled with a required reason.
- `EXECUTED` and `CANCELLED` are terminal and cannot be reopened.

No transition triggers bank execution or wallet mutation.

### Receivable

```text
OPEN -> COLLECTED
  +--> WAIVED
```

- Receivables do not enter payable review.
- `COLLECTED` requires an external collection reference, collection time, and remark.
- `WAIVED` requires a dedicated permission and a mandatory reason.
- `COLLECTED` and `WAIVED` are terminal and cannot be reopened.

### Balanced

`BALANCED` instructions are created directly in terminal `NO_ACTION`. They expose the source audit link but no financial action.

## Backend Architecture

Create a dedicated instruction mapper, service, controller, state-transition component, projection contracts, and persistence contracts inside `gameluck-payment`. Do not extend the Phase 45 settlement command service or the Phase 46 report query service.

Generation reads the closed settlement batch through tenant-scoped queries, validates its immutable snapshot, derives direction and initial status, and inserts the instruction plus creation action atomically. The database unique constraint resolves concurrent generation attempts.

Every command:

1. resolves the current tenant and actor;
2. validates permission-scoped input and the client request key;
3. loads the tenant-visible instruction;
4. returns the original result for an already completed request key;
5. validates direction, state, actor separation, required evidence, and expected version;
6. updates through an optimistic version predicate and appends exactly one action log in one transaction;
7. returns a string-safe projection.

The service never calls wallet, purchase-payment command, reconciliation mutation, settlement command, or member-risk mutation services.

## Admin API And Permissions

Use `/payment/settlement-instruction` as the resource root.

Read operations:

```text
GET /payment/settlement-instruction/list
GET /payment/settlement-instruction/{instructionId}
GET /payment/settlement-instruction/{instructionId}/actions
```

Commands:

```text
POST /payment/settlement-instruction/from-batch/{batchId}
PUT  /payment/settlement-instruction/{instructionId}
POST /payment/settlement-instruction/{instructionId}/submit
POST /payment/settlement-instruction/{instructionId}/approve
POST /payment/settlement-instruction/{instructionId}/reject
POST /payment/settlement-instruction/{instructionId}/cancel
POST /payment/settlement-instruction/{instructionId}/execute
POST /payment/settlement-instruction/{instructionId}/collect
POST /payment/settlement-instruction/{instructionId}/waive
```

Command bodies contain an `expectedVersion`, a bounded client `requestKey`, and only action-specific structured fields. Execution/collection time cannot precede the applicable approval or creation boundary and cannot exceed the accepted server-time tolerance.

Dedicated permissions are:

```text
payment:settlementInstruction:list
payment:settlementInstruction:query
payment:settlementInstruction:create
payment:settlementInstruction:edit
payment:settlementInstruction:review
payment:settlementInstruction:cancel
payment:settlementInstruction:execute
payment:settlementInstruction:collect
payment:settlementInstruction:waive
```

Sensitive command logs disable raw request/response persistence. Business action logs store only explicitly sanitized evidence.

## Validation And Stable Failures

Stable localized failures cover:

- absent or cross-tenant settlement batch/instruction;
- source batch not closed;
- duplicate instruction generation;
- invalid source snapshot, currency, or amount;
- direction/action or state/action mismatch;
- creator attempting self-review;
- missing or oversized reason, external reference, or remark;
- unmasked destination input or prohibited sensitive payload;
- stale expected version and concurrent transition conflict;
- duplicate request key with a different command payload;
- invalid external event time;
- attempts to reopen or change terminal records.

Validation failures and transaction rollbacks do not partially update an instruction or append a misleading success action.

## Admin Workbench

Payment Center gains `Settlement Instructions` immediately after `Settlement Report`. The primary screen is a unified dense list rather than separate direction tabs.

Filters include instruction/settlement number, direction, status, Provider, currency, and created-date range. The table shows instruction and source number, Provider, direction, currency, exact amount, status, creator/reviewer, relevant time, and one permission-aware primary action.

The detail drawer contains:

- immutable source settlement snapshot and a Phase 45 detail link;
- direction, exact amount, status, and version;
- counterparty, external method, and masked destination reference;
- current validation checks and only actions allowed by direction, state, actor, and permission;
- a chronological append-only action timeline.

Generation can start from the workbench by selecting a tenant-visible closed batch and from Phase 45/46 through a source link. A successful duplicate generation resolves to the existing instruction rather than creating another record.

The UI includes loading, empty, filtered-empty, permission-denied, network error/retry, validation error, version-conflict refresh, duplicate-submit, command-progress, success, and command-failure states. Color is never the only signal for direction or status.

At 390 px, filters form one column, the wide table scrolls only inside its own region, the drawer uses the viewport width, long identifiers wrap or truncate with tooltips, and the action region remains reachable without covering content.

## Security And Read-Only Boundaries

Phase 47 may mutate only instruction and instruction-action-log tables plus normal sanitized Admin operation logs. It must not change:

- payment sessions, webhook events, purchase orders, or reversals;
- reconciliation batches, lines, issues, or action evidence;
- settlement batches, items, or settlement action logs;
- Phase 46 report data, which remains derived from closed batches;
- member risk, turnover tasks, wallet accounts, or wallet transactions.

No response, log, screenshot, or test fixture exposes credentials, full account numbers, raw webhook bodies, signatures, private keys, or database connection secrets.

## Testing And Acceptance

Backend TDD covers schema constraints, string-safe contracts, direction derivation, exact amounts, tenant isolation, closed-source enforcement, duplicate and concurrent generation, every allowed and rejected transition, creator/reviewer separation, rejected-item resubmission, terminal protection, optimistic locking, request replay, changed-payload replay rejection, rollback behavior, and absence of forbidden service dependencies.

Admin contract and component checks cover exact endpoints, Blob-free structured commands, permissions, string-safe money/IDs, filter normalization, all visible states, source drill-down, action gating, conflict refresh, and responsive containment.

Runtime acceptance creates or reuses three closed batches producing `PAYABLE`, `RECEIVABLE`, and `BALANCED`. It verifies:

- one instruction per batch and exact source snapshots;
- rejection, revision, resubmission, approval, and external execution;
- creator self-review denial;
- collection and separately authorized waiver behavior;
- balanced no-action behavior;
- idempotent replay and concurrent conflict handling;
- permission and tenant isolation;
- desktop and 390 px layouts without page-level overflow;
- unchanged checksums for Phase 45/46 sources, payments, reconciliation, reversals, member risk, turnover, and wallets.

Final verification runs focused Phase 45/46 and payment-recovery regressions, the backend package, Admin and H5 production-equivalent builds, i18n, targeted ESLint, permission/secret/mutation scans, and `git diff --check`.

## Exclusions

- Real Provider, bank, ACH, wire, card, or treasury APIs.
- Automatic payout execution, wallet transfers, or bank-file generation.
- Full bank-account storage, payout credentials, beneficiary onboarding, or KYC/KYB.
- Partial payment, split beneficiaries, netting across batches, currencies, Providers, or tenants.
- General-ledger posting, invoices, tax, reserves, rolling holds, or FX.
- Reopening terminal instructions or editing closed settlement truth.
- Scheduled instruction generation, batch approval, bulk execution, email delivery, or background jobs.
