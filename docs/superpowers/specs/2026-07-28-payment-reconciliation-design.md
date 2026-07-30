# Payment Reconciliation And Discrepancy Review Design

## 1. Goal

Phase 44 adds operator-managed payment reconciliation based on uploaded Provider CSV statements. It compares Provider-declared payment events with platform payment sessions, purchase orders, webhook events, and reversals, then creates auditable discrepancies for manual classification.

The phase reconciles event identity, amount, currency, and status. It does not calculate settlement fees, net settlement amounts, foreign-exchange differences, or payout schedules.

## 2. Scope Boundaries

Included:

- UTF-8 CSV upload with strict schema, size, and row limits.
- Immutable reconciliation batches and normalized statement lines.
- Matching for payment success, failure, cancellation, refund, and chargeback events.
- Matched results, invalid rows, and operator-reviewable discrepancy records.
- Admin batch, detail, discrepancy, and resolution workflows.
- Tenant isolation, permissions, operation logs, business action logs, and idempotent SQL.

Excluded:

- Real Provider APIs or SDKs.
- Automatic order, session, webhook, reversal, turnover, or wallet mutation.
- Synthetic webhook creation.
- Settlement fees, net amounts, exchange rates, and settlement cycles.
- Partial refunds, repeated partial chargebacks, and automated discrepancy repair.

## 3. Architecture

The reconciliation domain remains inside `gameluck-payment`. Controllers accept files and query/action commands. Services own parsing, matching, state transitions, transaction boundaries, and audit records. Mappers only perform tenant-scoped persistence.

The domain has four tables:

### 3.1 Reconciliation Batch

`gl_payment_reconciliation_batch` represents one imported statement and stores:

- tenant, Provider, statement date, original file name;
- SHA-256 file digest;
- total, valid, invalid, matched, and discrepancy counts;
- status, failure reason, creator, and timestamps;
- version for guarded transitions.

The unique import identity is `(tenant_id, provider_code, file_digest)`. A byte-identical statement cannot be imported twice for the same tenant and Provider.

### 3.2 Reconciliation Line

`gl_payment_reconciliation_line` immutably stores each normalized CSV row:

- batch and source row number;
- Provider record ID and event type;
- Provider session number and purchase order number;
- payment currency, amount, and occurrence time;
- parse status, parse error, and a canonical JSON array of the original parsed field values.

Normalized business fields and the canonical original-field array are never updated after insertion. The array preserves every parsed field value without pretending that a quoted record containing embedded newlines maps to one physical source line.

### 3.3 Reconciliation Issue

`gl_payment_reconciliation_issue` stores one primary discrepancy per valid line or one platform-side missing-record discrepancy. It includes:

- discrepancy type and status;
- related statement line, session, order, webhook event, and reversal identifiers;
- Provider and platform comparison values;
- an immutable diagnostic snapshot containing all detected differences;
- resolution type, required remark, resolver, resolution time, and version.

### 3.4 Reconciliation Action Log

`gl_payment_reconciliation_action_log` is append-only. It records upload, validation, execution, failure, resolution, and ignore actions with before/after business states, operator, remark, and time.

No table stores Provider secrets, payment credentials, card data, or customer payment instruments.

## 4. State Machines

Batch states:

```text
UPLOADED -> VALIDATED -> RECONCILING -> COMPLETED
     |          |             |
     +----------+-------------+-> FAILED
```

- `UPLOADED`: file metadata and digest are accepted.
- `VALIDATED`: all lines have been parsed and counts are final.
- `RECONCILING`: one guarded execution owns the batch.
- `COMPLETED`: matching and issue creation are final.
- `FAILED`: an infrastructure or system error prevented completion.

If any line is invalid, the batch remains `VALIDATED` and execution is rejected. Operators correct the source and upload a new file; existing lines are not edited.

Issue states:

```text
OPEN -> RESOLVED
  +----> IGNORED
```

`RESOLVED` and `IGNORED` are terminal. Corrections require a later batch or an additional append-only audit record, never mutation of the original resolution.

## 5. CSV Contract

The file must be UTF-8, may contain a UTF-8 BOM, must be no larger than 10 MiB, and may contain no more than 50,000 data rows.

Required header order:

```text
provider_record_id,event_type,provider_session_no,purchase_order_no,pay_currency_code,pay_amount,occurred_time
```

Supported event types:

- `PAYMENT_SUCCEEDED`
- `PAYMENT_FAILED`
- `PAYMENT_CANCELLED`
- `REFUND_SUCCEEDED`
- `CHARGEBACK_CREATED`

The parser uses a structured CSV library already available in the project dependency graph, or a JDK-compatible parser if one is already established locally. It must support quoted commas, escaped quotes, CRLF/LF, BOM, and empty trailing fields. No ad hoc comma splitting is allowed.

Validation rejects missing or duplicate headers, unsupported events, blank required identities, invalid decimals, invalid currencies, invalid timestamps, and duplicate Provider record IDs within the file. Blank physical lines are ignored. A line error does not prevent other lines from being parsed, but any invalid line blocks reconciliation execution for the batch. Each stored source record contains its parser-reported source line number and a canonical JSON array of the unnormalized parsed fields; the service does not reconstruct or claim byte-exact CSV row text.

Money is parsed as `BigDecimal` and persisted as `DECIMAL(20,6)`. Currency codes are normalized to uppercase after strict syntax validation. `occurred_time` must be an ISO-8601 timestamp with an explicit offset and is normalized to UTC.

## 6. Matching Rules

Each Provider line is evaluated in this order:

1. Resolve the payment session by tenant, Provider, and Provider session number.
2. Validate the purchase order identity.
3. Compare currency and amount exactly using `BigDecimal.compareTo`.
4. Resolve the webhook event by tenant, Provider, and Provider record ID, then compare event type.
5. Compare the declared event against session, order, payment event, reversal, and review disposition state.

The engine also scans platform events in the batch statement-date window to identify `PROVIDER_RECORD_MISSING`. The window is the UTC calendar day represented by `statement_date`; no tenant-local timezone inference is applied in Phase 44.

Every valid Provider line produces either `MATCHED` or one primary issue. When multiple differences exist, primary issue priority is:

1. missing or ambiguous identity;
2. order identity;
3. currency;
4. amount;
5. missing event;
6. status;
7. unsupported record.

All detected differences remain in the diagnostic snapshot even when only one primary type is indexed.

## 7. Discrepancy Types

- `PLATFORM_RECORD_MISSING`: no unique platform session or order can be resolved.
- `PROVIDER_RECORD_MISSING`: an expected platform event in the statement window has no CSV row.
- `ORDER_IDENTITY_MISMATCH`: session identity resolves but order number differs.
- `AMOUNT_MISMATCH`: Provider and platform amounts differ.
- `CURRENCY_MISMATCH`: Provider and platform currencies differ.
- `EVENT_MISSING`: session and order exist but the corresponding webhook event is absent.
- `STATUS_MISMATCH`: Provider declaration conflicts with platform session, order, payment-event, reversal, or disposition state.
- `DUPLICATE_PROVIDER_RECORD`: duplicate Provider identity is detected in the statement.
- `UNSUPPORTED_RECORD`: the row is structurally valid but cannot be reconciled under Phase 44 rules.

Duplicate Provider IDs inside a file are line-validation errors and block execution. `DUPLICATE_PROVIDER_RECORD` is used when a valid imported identity conflicts with existing platform or prior-statement evidence that is not the same file digest.

## 8. Manual Resolution

An operator may resolve an open issue with exactly one classification:

- `PLATFORM_CONFIRMED`
- `PROVIDER_CONFIRMED`
- `EXPECTED_DIFFERENCE`
- `DUPLICATE_CONFIRMED`
- `OTHER`

A nonblank remark is mandatory. Operators may instead mark an issue `IGNORED`, also with a mandatory remark.

Resolution only records an operational conclusion. It never changes payment sessions, purchase orders, webhook events, payment events, reversals, turnover tasks, member risk, or wallet data.

The UI may link to existing payment order, payment session, webhook event, and reversal-review pages. Any retry or recovery action remains inside those existing permission boundaries and workflows.

## 9. Admin Experience

Payment Center gains a `Payment Reconciliation` workbench.

Batch list:

- Provider, statement date, file, digest summary, status, counts, creator, and timestamps.
- Filters for Provider, date, status, and file name.
- Upload command and permission-scoped execute command.

Upload dialog:

- Provider, statement date, and CSV file.
- Client-side file type and size feedback, with server validation authoritative.

Batch detail:

- unframed summary band;
- tabs for invalid lines, matched lines, and discrepancies;
- pagination and filters for discrepancy type, status, order, session, and Provider record ID.

Issue detail drawer:

- side-by-side Provider/platform values on desktop and one-column descriptions below 768 px;
- complete diagnostic snapshot and read-only canonical source-field array;
- related-record links and append-only action history;
- resolve/ignore confirmation with required classification and remark;
- explicit confirmation copy that no order or wallet value will change.

Operational tables remain horizontally scrollable on narrow viewports. Raw statement files and bulk sensitive-data exports are not exposed.

## 10. Permissions And Audit

Permissions:

```text
payment:reconciliation:list
payment:reconciliation:query
payment:reconciliation:upload
payment:reconciliation:execute
payment:reconciliation:resolve
```

Upload, execute, resolve, and ignore commands use Admin `@Log` operation logging. The business action-log table additionally records domain state transitions and mandatory remarks. Read APIs remain tenant-scoped and data-permission aware.

## 11. Transaction And Concurrency Boundaries

- Upload creates the batch and file digest record without running reconciliation.
- Validation parses in bounded batches and finalizes counts. An infrastructure failure transitions the batch to `FAILED` in a separate transaction so failure evidence survives rollback.
- Execute uses a guarded `VALIDATED -> RECONCILING` transition. Only one caller can own a batch. Matching, line conclusions, issues, counts, and `COMPLETED` transition are atomic.
- Issue resolution uses a guarded `OPEN -> RESOLVED/IGNORED` update with version checking. Concurrent losers receive a stable conflict response and create no action log.
- CSV content and original rows are never written to application logs. Errors do not expose server paths, SQL, or stack traces.

## 12. API Surface

Admin endpoints:

```text
GET  /payment/reconciliation/list
POST /payment/reconciliation/upload
GET  /payment/reconciliation/{batchId}
GET  /payment/reconciliation/{batchId}/lines
GET  /payment/reconciliation/{batchId}/issues
POST /payment/reconciliation/{batchId}/execute
GET  /payment/reconciliation/issues/{issueId}
POST /payment/reconciliation/issues/{issueId}/resolve
POST /payment/reconciliation/issues/{issueId}/ignore
```

Upload uses `multipart/form-data`. All identifiers in responses are strings where JavaScript integer precision could be lost. Monetary values are serialized as strings.

## 13. Testing And Acceptance

Backend tests cover:

- BOM, quoted commas, escaped quotes, CRLF/LF, blank lines, duplicate rows, header errors, invalid types, decimals, currencies, and timestamps;
- 10 MiB and 50,000-row limits without unbounded memory use;
- file digest idempotency and tenant isolation;
- five event types and all discrepancy types;
- primary discrepancy priority and complete diagnostic snapshots;
- exact decimal comparison and UTC statement-date boundaries;
- concurrent batch execution and concurrent issue resolution;
- failure persistence without partial issues;
- absence of wallet mapper/service dependencies in reconciliation controllers and services.

Frontend checks cover typed API contracts, permissions, labels, bilingual copy, read-only raw rows, mandatory remarks, terminal issue behavior, and safe related-record links.

Runtime acceptance imports a mixed statement containing matched rows, invalid rows, and each supported discrepancy. Because invalid rows block execution, acceptance uses one invalid-file batch to verify validation behavior and a corrected batch to verify matching. Operators resolve and ignore representative issues, verify operation/action logs, and confirm that order, session, webhook, reversal, turnover, member risk, and wallet snapshots remain unchanged.

SQL is imported twice and must produce one set of tables, menus, permissions, dictionary types, and dictionary rows. Desktop and 390 px browser evidence must show readable filters, tables, detail drawers, confirmation dialogs, and no page-level overflow.
