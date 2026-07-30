# Phase 46 Payment Settlement Report And Export Design

## 1. Goal

Phase 46 turns the immutable Phase 45 settlement batches into an operator-readable financial report and a bounded CSV export. It gives finance and payment operations a reliable way to review closed settlement results by UTC date, Provider, and currency without recalculating or changing payment state.

The report is a read-only projection. It must not update payment sessions, webhook events, purchase orders, reversals, reconciliation evidence, settlement batches, member risk, turnover tasks, wallet accounts, or wallet transactions.

## 2. Scope

Phase 46 delivers:

- an Admin settlement-report page under Payment Center;
- daily aggregation of `CLOSED` Phase 45 settlement batches;
- filtering by UTC date range, Provider, and currency;
- per-currency totals and drill-down to existing settlement batches;
- a synchronous UTF-8 CSV export of the current filter;
- dedicated list, query, and export permissions;
- tenant isolation, bilingual metadata, focused tests, builds, and runtime acceptance.

The maximum inclusive date range is 31 UTC calendar days. The default range is the latest seven UTC calendar days including today.

## 3. Approaches Considered

### 3.1 Query-Time Aggregation From Closed Batches (Selected)

Aggregate the immutable `gl_payment_settlement_batch` rows at query and export time. This keeps Phase 46 read-only, avoids duplicate financial truth, and guarantees that report totals reconcile to the closed batches operators can inspect.

The bounded 31-day window and indexed tenant/status/date filters keep the workload predictable. This is the smallest design that provides a runnable operational loop.

### 3.2 Persisted Daily Summary Table

A separate summary table would make broad reporting faster, but it introduces refresh scheduling, repair semantics, source-version tracking, and a second financial representation. Those costs are not justified for the current local platform scale.

### 3.3 Client-Side Aggregation

Returning raw batches and aggregating in Admin UI would duplicate business rules, weaken export consistency, and expose pagination errors. Financial aggregation remains server-owned.

## 4. Reporting Grain And Financial Rules

One summary row represents:

```text
tenant + UTC calendar date + provider_code + currency_code
```

The source set contains only Phase 45 batches whose status is `CLOSED` and whose `period_start` UTC calendar date is inside the requested inclusive date range. A settlement batch belongs to exactly one report date: the UTC date of `period_start`. Its period may cross midnight, but its immutable totals must never be split or prorated across dates.

Each row contains:

- UTC report date;
- Provider and currency;
- closed batch count and total included event count;
- payment event count, refund event count, and chargeback event count;
- gross payment, refund, chargeback, total fee, and net settlement totals;
- negative-net indicator;
- earliest period start, latest period end, and latest close time.

All counts use integer addition. Monetary totals use database decimal aggregation at the existing six-decimal precision and are serialized to the frontend as plain decimal strings. No currency conversion or cross-currency grand total is allowed.

The report footer groups totals by currency. Provider values may be combined within the same currency footer, but different currencies remain separate. Empty results return an empty row list and empty currency totals rather than zero-valued synthetic rows.

## 5. Backend Architecture

### 5.1 Read Model

Add a settlement-report query service inside `gameluck-payment`. It depends on a dedicated read-only mapper over `gl_payment_settlement_batch`; it does not call settlement command services or payment/wallet services.

The mapper applies all filters in SQL:

- current tenant;
- `status = CLOSED`;
- `period_start >= startDate at 00:00:00 UTC`;
- `period_start < dayAfterEndDate at 00:00:00 UTC`;
- optional exact Provider and currency filters.

The database groups and orders rows by report date descending, Provider ascending, and currency ascending. Pagination is applied to grouped rows, not source batches. Currency footer totals are calculated from the full filtered source set and are not limited to the current page.

### 5.2 Query API

```text
GET /payment/settlement-report/list
GET /payment/settlement-report/{date}/{providerCode}/{currencyCode}/batches
GET /payment/settlement-report/export
```

`list` returns the paged daily rows plus full-filter currency totals. The batch drill-down endpoint returns existing settlement batch projections for the exact tenant/date/Provider/currency group and uses the same source-membership rule as the report.

All JavaScript-sensitive identifiers remain strings. Money remains decimal strings. Dates are ISO `yyyy-MM-dd`; timestamps are ISO date-time values.

### 5.3 Validation

The server rejects:

- missing, invalid, or reversed dates;
- a range longer than 31 inclusive UTC dates;
- future end dates;
- unsupported Provider or currency values;
- page sizes outside the existing platform limits;
- drill-down keys that do not identify a visible report group.

Cross-tenant data is treated as absent. Stable localized business messages are returned without SQL, stack traces, filesystem paths, or internal secrets.

## 6. CSV Export

Export applies exactly the same validated filters, grouping, ordering, source-membership rule, and decimal formatting as the screen query. It exports the complete filtered result, independent of screen pagination, with a hard maximum of 2,000 grouped rows. A result beyond that limit is rejected and instructs the operator to narrow the date or dimension filters.

The response is streamed synchronously as UTF-8 with BOM and `Content-Type: text/csv; charset=UTF-8`. The deterministic filename is:

```text
payment-settlement-report_<start-date>_<end-date>.csv
```

Columns are fixed and bilingual-neutral at the data-contract level:

```text
report_date,provider_code,currency_code,batch_count,event_count,
payment_event_count,refund_event_count,chargeback_event_count,
gross_payment,refund_amount,chargeback_amount,total_fee,net_settlement,
negative_net,earliest_period_start,latest_period_end,latest_close_time
```

Every text cell is CSV-escaped with a structured writer. Before escaping, values whose first non-whitespace character is `=`, `+`, `-`, or `@` receive a leading apostrophe to prevent spreadsheet formula execution. Numeric money and count columns are emitted only from typed server values, never from untrusted text. Newlines, commas, quotes, control characters, and nulls are handled by the CSV library rather than manual concatenation.

The export endpoint does not create files on the application server, database rows, audit payload copies, or background jobs. The existing Admin operation log records actor, endpoint, sanitized filters, result, and time; it must not record response bytes.

## 7. Permissions And Audit

Permissions are:

```text
payment:settlementReport:list
payment:settlementReport:query
payment:settlementReport:export
```

The page menu requires `list`. Drill-down requires `query`. The export button and endpoint require `export`. Backend authorization is authoritative even when the frontend hides a command.

List and drill-down are read operations. Export uses Admin operation logging with an export business type and records only sanitized filters and success/failure metadata.

## 8. Admin Experience

Payment Center gains `Settlement Report` after `Payment Settlement`. The page is a dense operational report rather than a dashboard landing page.

The filter bar contains UTC start date, UTC end date, Provider, and currency. Quick date controls offer the latest 7 and 31 UTC days. Search, reset, and export commands remain visible at desktop widths; on narrow screens they wrap without overlapping.

The primary table shows report date, Provider, currency, batch and event counts, gross payment, refunds, chargebacks, fees, net settlement, and latest close time. Negative net values use a restrained warning treatment plus explicit text; color is not the only signal. Clicking the batch count opens an unframed drawer listing the source Phase 45 batches with links to their existing settlement details.

Currency totals appear in a full-width summary band above the table. Each currency is a peer summary item, not a cross-currency total. Empty, loading, error, unauthorized, no-export-permission, and export-in-progress states are explicit.

At 390 px, filters use one column, currency summaries stack, command labels remain readable, and the wide report table scrolls inside its own region without page-level horizontal overflow.

## 9. Data Integrity And Concurrency

Reports use ordinary read-only queries and do not lock settlement rows. Because `CLOSED` batches are terminal and their financial fields are immutable, repeated queries over the same filter produce the same values unless additional batches become closed in that range.

Each request uses a single database-consistent query for grouped rows and another for currency totals. The response includes a server `generatedAt` timestamp so operators can identify the observation time. Export executes its grouped query once and writes that result; it does not combine separately paged reads.

No report result is presented as an accounting ledger, payout instruction, or evidence that a Provider transferred funds.

## 10. Testing And Acceptance

Backend tests cover:

- closed-only source selection;
- UTC date membership, midnight boundaries, and crossed-midnight batches;
- tenant, Provider, and currency isolation;
- grouped counts and six-decimal financial totals;
- negative net values and per-currency footers;
- pagination after grouping;
- empty results and all validation failures;
- drill-down membership and cross-tenant absence;
- export parity, deterministic ordering, row limit, UTF-8 BOM, quoting, and formula-injection protection;
- permission annotations and absence of wallet/payment command dependencies.

Frontend checks cover typed string identifiers and money, query defaults, permission-scoped export, bilingual labels, loading/empty/error states, negative-net visibility, drill-down links, and responsive containment.

Runtime acceptance creates or reuses multiple closed Phase 45 batches across dates, Providers, and currencies. It verifies exact report and currency totals, filter isolation, batch drill-down, screen/export parity, a malicious text value rendered inert in CSV, unauthorized export rejection, desktop and 390 px layouts, and unchanged source-table snapshots.

SQL imports twice without duplicate menus or permissions. Focused and cross-module tests, backend package, Admin build, H5 build, service HTTP checks, and `git diff --check` must pass.

## 11. Non-Goals

- Real Provider APIs, credentials, statement downloads, payouts, or bank execution.
- Scheduled reports, email delivery, background export jobs, or object storage.
- Persisted daily summary tables, materialized views, or report repair workflows.
- General-ledger entries, invoices, tax, reserves, or accounting-period close.
- FX conversion, base-currency totals, or cross-currency netting.
- Editing fee rules, reconciliation evidence, or closed settlement data.
- Exporting raw webhook bodies, signatures, secrets, member data, or wallet data.
