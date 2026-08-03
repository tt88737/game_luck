# Phase 48 Payment Settlement Instruction Aging Monitor Design

## 1. Purpose

Phase 48 gives payment and finance operators a tenant-scoped view of aging, approaching deadlines, overdue exposure, and workflow stalls across Phase 47 settlement instructions. It helps operators find work that needs attention and then opens the existing Phase 47 instruction workflow to resolve it.

The monitor derives its results at query time. It does not create a second exception lifecycle, change instruction status, execute a payment, collect funds, move wallet balances, or post accounting entries.

## 2. Scope

Phase 48 delivers:

- one versioned aging policy per tenant, with stable system defaults when no override exists;
- UTC natural-day deadlines for payable execution, receivable collection, and payable workflow stages;
- query-time `NORMAL`, `DUE_SOON`, `OVERDUE`, and `STALLED` classifications;
- aging buckets, remaining or overdue days, and per-currency exposure summaries;
- a permission-scoped Admin monitoring workbench with navigation to Phase 47 instructions;
- desktop and 390 px responsive behavior, focused tests, builds, and runtime acceptance.

Phase 48 does not add exception assignment, acknowledgement, ignore, snooze, notifications, scheduled snapshots, background jobs, exports, real Provider or bank integration, treasury movement, wallet mutation, or accounting-ledger posting.

## 3. Approaches Considered

### 3.1 Query-Time Derivation With Persisted Policy (Selected)

Persist only the tenant SLA policy and derive every monitoring result from the current Phase 47 instruction state and server time. This avoids a second financial or operational truth, makes resolved instructions disappear automatically, and keeps Phase 47 command behavior unchanged.

### 3.2 Scheduled Exception Snapshots

A scheduled snapshot table could make broad reporting and future notifications cheaper, but it requires scheduling, retry, repair, deduplication, and synchronization semantics. Those costs are not justified for the current bounded operational workload.

### 3.3 Writing Monitoring Fields Back To Instructions

Persisting overdue or stalled flags on Phase 47 instructions would couple derived time state to the instruction state machine and require continuous updates. Phase 48 rejects this approach.

## 4. Aging Policy

Add `gl_payment_settlement_aging_policy` with:

- tenant ID and policy ID;
- payable execution SLA days;
- receivable collection SLA days;
- payable draft SLA days;
- payable rejected-revision SLA days;
- payable pending-review SLA days;
- due-soon warning days;
- optimistic-lock version and standard create/update audit fields.

There is at most one active row per tenant. A tenant without a row uses these system defaults:

| Rule | Default |
| --- | ---: |
| Payable execution | 2 natural days |
| Receivable collection | 7 natural days |
| Payable draft | 2 natural days |
| Payable rejected revision | 2 natural days |
| Payable pending review | 1 natural day |
| Due-soon warning | 1 natural day |

All values are bounded non-negative integers. Execution, collection, and workflow SLA values must be between 1 and 90 inclusive. The due-soon warning must be between 0 and 30 inclusive. The API returns whether the effective policy comes from system defaults or a tenant override.

Policy updates use an expected version. A concurrent update returns a stable version-conflict error and does not overwrite the newer policy. Updating a policy affects subsequent queries only; it does not rewrite instructions or preserve historical monitoring classifications.

## 5. UTC Natural-Day Semantics

All deadline and age calculations use UTC calendar dates, not the browser timezone or elapsed 24-hour windows.

For a base timestamp on UTC date `D` and SLA `N`, the instruction remains within SLA through `23:59:59.999 UTC` on date `D + N`. It becomes overdue or stalled at the start of the next UTC date. For example, an instruction approved on August 1 with a two-day payable execution SLA is due through August 3 UTC and becomes overdue on August 4 UTC.

One list request captures one server-side `evaluatedAt` value. Paged rows and full-filter summaries use the same value so a request cannot classify records against different dates.

## 6. Classification Rules

Only active Phase 47 instructions enter the monitor. Terminal `EXECUTED`, `COLLECTED`, `WAIVED`, `CANCELLED`, and `NO_ACTION` instructions are excluded from active monitoring and exposure summaries.

### 6.1 Payable Execution

An `APPROVED` payable uses its approval timestamp as the base time and the payable execution SLA. It is:

- `OVERDUE` after its UTC deadline;
- `DUE_SOON` when it is not overdue and its deadline is within the configured warning window;
- `NORMAL` otherwise.

### 6.2 Receivable Collection

An `OPEN` receivable uses its instruction creation timestamp as the base time and the receivable collection SLA. It uses the same `OVERDUE`, `DUE_SOON`, and `NORMAL` rules.

### 6.3 Payable Workflow Stalls

Payable workflow records use stage-specific rules:

- `DRAFT` uses the instruction creation timestamp and draft SLA;
- `REJECTED` uses the latest successful rejection action time and rejected-revision SLA;
- `PENDING_REVIEW` uses the latest successful submission action time and pending-review SLA.

After the stage deadline, these records are `STALLED`. Before the deadline they are `DUE_SOON` or `NORMAL` using the same warning-window rule. They never become `OVERDUE`, which is reserved for approved payable execution and open receivable collection exposure.

Classification precedence is `OVERDUE`, then `STALLED`, then `DUE_SOON`, then `NORMAL`. Missing or inconsistent required timestamps are rejected as stable data-integrity failures rather than silently assigned a misleading classification.

## 7. Aging Projection And Summaries

Each monitoring row includes:

- string-safe instruction and settlement batch IDs and numbers;
- direction, current Phase 47 status, Provider, and currency;
- exact signed net settlement and instruction amount as decimal strings;
- applicable base time, base UTC date, deadline UTC date, and evaluated time;
- age in natural days, remaining days or overdue/stalled days;
- aging bucket and derived monitoring classification;
- relevant creator, submitter, reviewer, or outcome operator evidence where available.

The UI must not imply ownership or assignment that Phase 47 does not model.

Aging buckets are stable and mutually exclusive:

```text
0-1 days
2-3 days
4-7 days
8+ days
```

Full-filter summaries include counts by derived classification, current Phase 47 status, direction, and aging bucket. Financial exposure is grouped by currency and classification. Different currencies are never added into one amount. Empty results return empty collections rather than synthetic zero-currency rows.

## 8. Backend Architecture And API

Create a dedicated aging-policy mapper/service and a read-only aging-monitor query mapper/service inside `gameluck-payment`. Do not add monitoring behavior to the Phase 47 instruction command service or Phase 45/46 settlement services.

The monitor query applies mandatory tenant isolation and active-status boundaries in SQL. It may use bounded SQL projections plus a focused Java classifier, but pagination and summaries must use identical predicates and deadline semantics. The implementation must not load an unbounded instruction set into application memory.

Use `/payment/settlement-aging` as the resource root:

```text
GET /payment/settlement-aging/list
GET /payment/settlement-aging/policy
PUT /payment/settlement-aging/policy
```

The list endpoint returns paged rows, full-filter summaries, the effective policy, and `evaluatedAt`. Filters support monitoring classification, Phase 47 status, direction, Provider, currency, aging bucket, and an exact or normalized instruction/settlement-number search. The default view includes `OVERDUE`, `STALLED`, and `DUE_SOON`; operators may explicitly include `NORMAL`.

Dedicated permissions are:

```text
payment:settlementAging:list
payment:settlementAging:query
payment:settlementAging:policyEdit
```

Monitoring permissions do not grant Phase 47 create, edit, review, execute, collect, waive, or cancel permissions. Opening an instruction uses the existing Phase 47 route and permission checks.

## 9. Validation And Stable Failures

Stable localized failures cover:

- invalid or out-of-range policy values;
- stale policy expected version;
- unsupported classification, status, direction, Provider, currency, or aging bucket filters;
- absent or cross-tenant policy and instruction data;
- inconsistent direction/status/timestamp evidence required for classification;
- page sizes outside existing platform limits;
- missing list, query, policy-edit, or Phase 47 navigation permission;
- unexpected query or persistence failure without exposing SQL, stack traces, paths, or secrets.

Policy save failure does not block read-only monitoring. Query failure does not display cached values as though they were current. A Phase 47 instruction changed by another operator is recomputed on the next list request and leaves the exception set automatically when terminal.

## 10. Admin Workbench

Payment Center gains `Settlement Aging Monitor` after `Settlement Instructions`. The desktop page uses one dense operational workbench:

1. title, UTC evaluation time, and permission-aware SLA settings command;
2. activity, overdue, stalled, and due-soon count summaries;
3. aging distribution and per-currency exposure summaries;
4. normalized filters;
5. a paged table with monitoring state, instruction/source identity, direction, currency and exact amount, current instruction status, age, deadline or delay, relevant operator, and an open-instruction action.

The page includes loading, no-active-instruction, filtered-empty, permission-denied, network-error/retry, policy-load-error, policy-saving, policy-save-success, validation-error, and version-conflict-refresh states. Text accompanies all status colors and negative financial direction.

At 390 px, the four count summaries use a stable two-by-two grid, currency exposure remains readable, secondary filters collapse behind a control, and the wide table scrolls only inside its own region. The page itself must not overflow horizontally. Long IDs wrap or truncate with tooltips, and controls must not cover content.

The workbench does not resolve, acknowledge, assign, ignore, or snooze an exception. Operators open the source Phase 47 instruction and use only actions allowed by its state, actor separation, and permissions.

## 11. Security And Mutation Boundaries

Phase 48 may mutate only the aging-policy table and normal sanitized Admin operation logs. It must not modify:

- settlement instructions or their append-only action logs;
- settlement batches, items, reports, or settlement action logs;
- reconciliation batches, lines, issues, or evidence;
- payment sessions, webhook events, purchase orders, or reversals;
- member risk, turnover tasks, wallet accounts, or wallet transactions.

No policy, response, log, screenshot, or fixture stores or exposes full bank-account details, credentials, raw webhook payloads, signatures, private keys, or database secrets.

## 12. Testing And Acceptance

Backend TDD covers policy defaults and tenant overrides, validation, optimistic locking, UTC date boundaries, every supported source state and base timestamp, deadline calculations, warning windows, classification precedence, aging buckets, terminal exclusion, string-safe IDs and money, tenant isolation, pagination/summary consistency, deterministic `evaluatedAt`, and forbidden dependency/write scans.

Admin contract and component tests cover exact endpoints and permissions, string-safe values, filter normalization, per-currency summaries, every visible state, policy conflicts, Phase 47 navigation gating, long identifiers, and responsive containment.

Runtime acceptance uses a controlled evaluation time and tenant-isolated fixtures for `NORMAL`, `DUE_SOON`, `OVERDUE`, and `STALLED` records. It verifies default and overridden policies, UTC boundary changes, exact per-currency summaries, terminal disappearance after a real Phase 47 transition, cross-tenant denial, policy version conflict, desktop layout, and 390 px table-local scrolling.

Deterministic before/after checksums prove that monitoring and policy tests leave Phase 45/46/47 sources, payments, reconciliation, reversals, member risk, turnover, and wallets unchanged. Only the aging-policy row and permitted sanitized operation logs may change.

Final verification runs focused Phase 45-47 and payment-recovery regressions, the backend package, Admin and H5 production-equivalent builds, i18n, targeted ESLint, permission/secret/mutation scans, `git diff --check`, and desktop/mobile screenshot inspection.

## 13. Exclusions

- Real Provider, bank, ACH, wire, card, treasury, or payout APIs.
- Automatic execution, collection, wallet transfers, or ledger posting.
- Notifications, email, webhook delivery, scheduled scans, or background jobs.
- Persisted exception snapshots or monitoring fields on Phase 47 instructions.
- Exception assignment, acknowledgement, ignore, snooze, escalation workflow, or comments.
- Cross-tenant, cross-currency, or cross-Provider netting.
- Full bank details, payout credentials, beneficiary onboarding, or KYC/KYB.
- CSV export, dashboards outside Payment Center, or historical classification replay.
