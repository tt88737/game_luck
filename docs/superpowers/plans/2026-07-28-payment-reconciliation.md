# Payment Reconciliation And Discrepancy Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped CSV payment-statement reconciliation, deterministic event matching, auditable discrepancy review, and an Admin operations workbench without mutating payment or wallet state.

**Architecture:** Keep reconciliation inside `gameluck-payment`. Persist immutable batches and normalized lines, execute a guarded matching engine against existing payment sessions/orders/webhooks/reversals, and store independent issues plus append-only action logs. Admin commands upload, execute, resolve, or ignore; none can create payment events or invoke wallet behavior.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, Hutool structured CSV APIs, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite, Playwright-compatible browser acceptance.

**Execution constraint:** Preserve the shared dirty `main` worktree. Do not create Git commits unless the user explicitly requests one; use review checkpoints instead of commit steps.

---

## File Map

- Persistence: reconciliation enums and four entities/mappers under `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment`, plus `backend/script/sql/gameluck_wallet.sql`.
- Parsing: `PaymentReconciliationCsvParser.java` and immutable parser result records under `service/reconciliation`.
- Application services: upload/query service, matching engine, execution coordinator, failure recorder, and resolution service under `service/impl` and `service/reconciliation`.
- Admin boundary: `PaymentReconciliationController.java`, BO/VO projections, permissions, i18n, menus, and dictionaries.
- Admin UI: typed APIs and `admin-ui/src/views/payment/payment-reconciliation/index.vue`.
- Verification: focused backend tests, frontend contract script, browser acceptance script, SQL/runtime evidence, `progress.md`, and `task_plan.md`.

### Task 1: Add Reconciliation Persistence Contracts

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentReconciliationBatchStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentReconciliationLineStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentReconciliationIssueStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentReconciliationIssueType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentReconciliationResolutionType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentReconciliationBatch.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentReconciliationLine.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentReconciliationIssue.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentReconciliationActionLog.java`
- Create: corresponding mapper interfaces under `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentReconciliationPersistenceContractTest.java`

- [x] **Step 1: Write the failing persistence contract**

Assert exact enum values:

```java
assertArrayEquals(new String[]{"UPLOADED", "VALIDATED", "RECONCILING", "COMPLETED", "FAILED"}, names(PaymentReconciliationBatchStatus.values()));
assertArrayEquals(new String[]{"VALID", "INVALID", "MATCHED", "ISSUE"}, names(PaymentReconciliationLineStatus.values()));
assertArrayEquals(new String[]{"OPEN", "RESOLVED", "IGNORED"}, names(PaymentReconciliationIssueStatus.values()));
assertArrayEquals(new String[]{"PLATFORM_RECORD_MISSING", "PROVIDER_RECORD_MISSING", "ORDER_IDENTITY_MISMATCH", "AMOUNT_MISMATCH", "CURRENCY_MISMATCH", "EVENT_MISSING", "STATUS_MISMATCH", "DUPLICATE_PROVIDER_RECORD", "UNSUPPORTED_RECORD"}, names(PaymentReconciliationIssueType.values()));
assertArrayEquals(new String[]{"PLATFORM_CONFIRMED", "PROVIDER_CONFIRMED", "EXPECTED_DIFFERENCE", "DUPLICATE_CONFIRMED", "OTHER"}, names(PaymentReconciliationResolutionType.values()));
```

Reflectively require every field from the approved design. Isolate each `CREATE TABLE` block from `gameluck_wallet.sql`; assert tenant-first unique keys, immutable raw-line storage, `DECIMAL(20,6)`, diagnostic JSON/LONGTEXT, version fields, and lookup indexes.

- [x] **Step 2: Run the persistence test and verify RED**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentReconciliationPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: test compilation fails only because reconciliation enums/entities/mappers do not exist.

- [x] **Step 3: Implement exact domain and mapper contracts**

Mapper methods must include tenant-scoped batch/list/detail reads, digest lookup, guarded batch transitions, batch line pagination, issue pagination/detail, `OPEN` guarded resolution, and ordered action-log reads. Required guards:

```java
int transitionStatus(String tenantId, Long id, String expectedStatus, String nextStatus, Date now);
int resolveOpenIssue(String tenantId, Long id, Integer expectedVersion, String nextStatus,
                     String resolutionType, String remark, Long operatorId, Date resolvedTime);
```

Use idempotent `information_schema` guards consistent with the existing wallet SQL. Do not add foreign keys or destructive alterations.

- [x] **Step 4: Run Task 1 GREEN**

Run Step 2 unchanged. Expected: all persistence contract tests pass with zero failures and errors.

- [x] **Step 5: Review checkpoint**

Confirm every unique business key starts with `tenant_id`, no table contains Provider secrets or payment instruments, and existing Phase 38-43 tables are not altered destructively.

### Task 2: Build The Bounded Structured CSV Parser

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/PaymentReconciliationCsvParser.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/ReconciliationParsedLine.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/ReconciliationParseResult.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/reconciliation/PaymentReconciliationCsvParserTest.java`

- [x] **Step 1: Write failing parser tests**

Cover exact header order, UTF-8 BOM, CRLF/LF, quoted commas, escaped quotes, blank physical lines, empty trailing fields, invalid UTF-8, missing/duplicate headers, unknown event, blank identity, invalid currency, invalid decimal, scale overflow, timestamp without offset, duplicate Provider IDs, 10 MiB limit, and 50,000-row limit.

Desired API:

```java
ReconciliationParseResult parse(InputStream input, long declaredSize);
```

Each result line includes the parser-reported source line number, normalized fields, a canonical JSON array of unnormalized parsed field values, `VALID/INVALID`, and one stable parse-error code. The result includes digest, total/valid/invalid counts, and bounded line records.

- [x] **Step 2: Run parser tests and verify RED**

Run Task 1 command with `-Dtest=PaymentReconciliationCsvParserTest`.

Expected: compilation fails on absent parser contracts.

- [x] **Step 3: Implement parser with Hutool structured CSV APIs**

Use `cn.hutool.core.text.csv.CsvReader`/`CsvRow` from the existing Hutool dependency. Wrap input in a counting/limit stream before decoding. Use a strict UTF-8 `CharsetDecoder` with malformed/unmappable input set to `REPORT`. Do not use `String.split`, regex CSV parsing, or unbounded `readAllBytes`.

Normalize with:

```java
BigDecimal amount = new BigDecimal(rawAmount).setScale(6, RoundingMode.UNNECESSARY);
String currency = rawCurrency.trim().toUpperCase(Locale.ROOT);
Instant occurredTime = OffsetDateTime.parse(rawOccurredTime).toInstant();
```

Hash the exact original bytes with SHA-256 while parsing. Reject the 50,001st data row and any byte beyond 10 MiB. Serialize `CsvRow.getRawList()` with the configured Jackson `ObjectMapper` for source evidence. Do not reconstruct byte-exact row text because quoted CSV records may contain embedded physical newlines.

- [x] **Step 4: Run parser tests GREEN**

Expected: all parser cases pass; the large-input test proves rejection without retaining bytes beyond configured bounds.

- [x] **Step 5: Review checkpoint**

Confirm raw CSV or source rows are never logged and parse errors contain no server path, SQL, or stack trace.

### Task 3: Upload, Validate, And Query Reconciliation Batches

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentReconciliationBatchBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentReconciliationBatchVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentReconciliationBatchDetailVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentReconciliationLineVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentReconciliationService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentReconciliationServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentReconciliationServiceImplTest.java`

- [x] **Step 1: Write failing upload/query tests**

Cover tenant-scoped upload, supported Provider validation through `PaymentProviderRegistry`, digest replay rejection, same digest allowed for another tenant, sanitized file name, invalid-line batch retained as `VALIDATED`, valid-line batch retained as `VALIDATED`, exact counts, paginated lines, and inaccessible cross-tenant IDs.

Desired commands:

```java
PaymentReconciliationBatchDetailVo upload(String providerCode, LocalDate statementDate,
                                            String originalFileName, long size, InputStream input);
TableDataInfo<PaymentReconciliationBatchVo> queryPage(PaymentReconciliationBatchBo bo, PageQuery pageQuery);
PaymentReconciliationBatchDetailVo queryDetail(Long batchId);
TableDataInfo<PaymentReconciliationLineVo> queryLines(Long batchId, String lineStatus, PageQuery pageQuery);
```

- [x] **Step 2: Run service tests and verify RED**

Run Task 1 command with `-Dtest=PaymentReconciliationServiceImplTest`.

- [x] **Step 3: Implement upload and read projections**

Create `UPLOADED`, parse, insert normalized immutable lines in bounded batches, then guarded-transition to `VALIDATED` with final counts. On duplicate `(tenant, provider, digest)`, return a stable localized error and do not insert lines. Persist infrastructure failure using a separate failure-recorder bean/transaction so the batch reaches `FAILED` after the upload transaction rolls back its incomplete lines.

Do not persist the complete file blob. Store file name, digest, normalized rows, parser source line numbers, and canonical original-field arrays only.

- [x] **Step 4: Run Task 3 GREEN**

Expected: upload/query service tests pass with exact tenant and digest behavior.

- [x] **Step 5: Review checkpoint**

Confirm invalid rows block future execution but remain queryable, and no upload API trusts client counts, digest, Provider identity normalization, or statement timestamps.

### Task 4: Implement Deterministic Matching And Issue Creation

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/PaymentReconciliationMatcher.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/ReconciliationMatchResult.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/ReconciliationDifference.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/reconciliation/ReconciliationPlatformSnapshot.java`
- Modify: payment session/order/webhook/reversal mappers only to add required tenant-scoped reconciliation projections
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/reconciliation/PaymentReconciliationMatcherTest.java`

- [x] **Step 1: Write failing matching tests**

Cover matched success/failure/cancel/refund/chargeback, all nine issue types, ambiguous public Provider-session identity, order mismatch, currency before amount precedence, amount before event/status precedence, missing webhook, reversal/review terminal mapping, duplicate prior-statement evidence, unsupported structurally valid records, and full diagnostic snapshots containing every detected difference.

Desired pure contract:

```java
ReconciliationMatchResult match(ReconciliationParsedLine line, ReconciliationPlatformSnapshot platform);
```

`ReconciliationMatchResult` contains `matched`, optional primary issue type, and an ordered immutable list of all differences. It has no mapper or wallet dependency.

- [x] **Step 2: Run matcher tests and verify RED**

Run Task 1 command with `-Dtest=PaymentReconciliationMatcherTest`.

- [x] **Step 3: Implement event-state matrix and priority**

Encode one explicit expected-state matrix for the five Provider event types. Use exact identity, uppercase currency, and `BigDecimal.compareTo`. Primary priority must be identity, order, currency, amount, event, status, unsupported.

Serialize the complete difference list with the configured Jackson `ObjectMapper`; do not hand-build JSON strings.

- [x] **Step 4: Add platform missing-record discovery tests**

Require a tenant/Provider/UTC-day query that returns platform webhook events expected in the statement and excludes events already represented by valid CSV Provider IDs. Assert midnight-inclusive and next-midnight-exclusive boundaries.

- [x] **Step 5: Implement platform projections and run GREEN**

All new mapper queries must include `tenant_id`; the only Provider-session lookup without a known tenant must return at most two candidates and the service must reject zero or multiple matches, matching Phase 43 public checkout hardening.

- [x] **Step 6: Review checkpoint**

Confirm matcher/services import no wallet package and never call payment mutation, reversal mutation, or webhook retry services.

### Task 5: Execute Batches With Durable Failure And Concurrency Guards

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentReconciliationExecutionService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentReconciliationFailureRecorder.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentReconciliationService.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentReconciliationExecutionServiceTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentReconciliationFailureRecorderTest.java`

- [x] **Step 1: Write failing execution tests**

Cover valid batch completion, invalid-line rejection, non-`VALIDATED` rejection, matched/issue counts, platform-missing issues, exact action logs, two-request concurrency with one winner, forced matcher/persistence exception rollback, and independent `FAILED` persistence.

- [x] **Step 2: Run execution tests and verify RED**

Run Task 1 command with both new test classes.

- [x] **Step 3: Implement three-stage execution**

```java
// transaction A: guarded VALIDATED -> RECONCILING
ExecutionLease acquire(Long batchId);
// transaction B: create conclusions/issues/log and RECONCILING -> COMPLETED atomically
PaymentReconciliationBatchDetailVo reconcile(ExecutionLease lease);
// transaction C, REQUIRES_NEW: RECONCILING -> FAILED after transaction B rollback
void recordFailure(String tenantId, Long batchId, String stableReason);
```

Annotate the production constructor of any bean with multiple constructors using `@Autowired`, preventing the Phase 43 startup defect from recurring.

- [x] **Step 4: Run execution tests GREEN**

Expected: concurrent loser receives a stable state-conflict error; forced failure leaves zero issues/conclusions but one `FAILED` batch and one failure action log.

- [x] **Step 5: Review checkpoint**

Confirm rerunning `COMPLETED` or `FAILED` batches cannot duplicate issues, and error text never includes CSV rows or internal exceptions.

### Task 6: Add Issue Resolution And Admin Backend APIs

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentReconciliationIssueBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentReconciliationResolutionBo.java`
- Create: issue/detail/action-log VOs under `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentReconciliationController.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentReconciliationService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentReconciliationServiceImpl.java`
- Modify: backend i18n bundles
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentReconciliationControllerContractTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentReconciliationResolutionServiceTest.java`

- [x] **Step 1: Write failing controller/resolution contracts**

Require the nine approved endpoints and exact permissions:

```text
payment:reconciliation:list
payment:reconciliation:query
payment:reconciliation:upload
payment:reconciliation:execute
payment:reconciliation:resolve
```

Require `@Log` on upload, execute, resolve, and ignore. Test nonblank remark, valid resolution type, `OPEN`-only handling, cross-tenant denial, terminal replay rejection, and concurrent resolution with one winner/action log.

- [x] **Step 2: Run Admin backend tests and verify RED**

Run Task 1 command with both new classes.

- [x] **Step 3: Implement APIs and resolution behavior**

Use `@RequestPart("file") MultipartFile file` plus Provider and ISO statement date for upload. Return string IDs and monetary strings in VOs. Resolve and ignore only write issue fields and append action logs; they must not inject or call payment session, purchase payment event, reversal, turnover, member, or wallet mutation services.

- [x] **Step 4: Add idempotent menu and dictionaries**

Add one Payment Center page after webhook events and before reversal review, plus query/upload/execute/resolve function permissions. Add dictionaries for batch status, line status, issue type/status, and resolution type using the established delete-and-insert idempotent pattern.

- [x] **Step 5: Run Task 6 GREEN**

Expected: controller and resolution suites pass, including exact permission/log annotations and zero state mutation outside reconciliation tables.

- [x] **Step 6: Review checkpoint**

Scan controller for mapper injection and service packages for wallet imports. Confirm operation log and business action log cover every mutation.

### Task 7: Build The Admin Reconciliation Workbench

**Files:**
- Create: `admin-ui/src/api/payment/paymentReconciliation/index.ts`
- Create: `admin-ui/src/api/payment/paymentReconciliation/types.ts`
- Create: `admin-ui/src/views/payment/payment-reconciliation/index.vue`
- Modify: `admin-ui/src/utils/businessLabels.ts`
- Modify: `admin-ui/src/utils/i18nTitle.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`
- Create: `admin-ui/scripts/check-payment-reconciliation-contract.mjs`
- Create: `admin-ui/scripts/phase44-payment-reconciliation-acceptance.mjs`

- [x] **Step 1: Write the failing frontend contract**

Require exact API paths, multipart upload, string IDs/money, permission strings, label groups, bilingual copy, terminal issue action hiding, required remarks, read-only canonical source fields/diagnostics, related payment links, and explicit no-wallet-mutation confirmation text.

- [x] **Step 2: Run contract and verify RED**

```powershell
node admin-ui/scripts/check-payment-reconciliation-contract.mjs
```

Expected: failure on absent API/view files.

- [x] **Step 3: Add typed APIs and labels**

Define explicit unions for all reconciliation enums. Use `FormData` for upload and existing request helpers for JSON commands/queries. All money remains `string`; all snowflake IDs remain `string`.

- [x] **Step 4: Implement operational workbench**

Use a dense batch table, upload dialog, detail drawer or unframed detail region, summary band, tabs for invalid/matched/issues, horizontally scrollable tables, and an issue detail drawer. Resolve/ignore use a focused confirmation dialog with classification select and mandatory remark. Do not use nested cards or expose raw-file download.

Required states: loading, empty, filtered empty, network error/retry, permission denied, upload validation failure, invalid-line execution block, reconciling/disabled, completed, failed, open issue, terminal issue, successful resolution, and concurrency conflict.

- [x] **Step 5: Run frontend checks GREEN**

```powershell
node admin-ui/scripts/check-payment-reconciliation-contract.mjs
corepack pnpm --dir admin-ui check:i18n
corepack pnpm --dir admin-ui exec eslint --rule 'prettier/prettier: off' src/api/payment/paymentReconciliation src/views/payment/payment-reconciliation/index.vue src/utils/businessLabels.ts src/utils/i18nTitle.ts
```

- [x] **Step 6: Browser acceptance checkpoint**

At 1440 px and 390 px capture upload validation, completed batch summary, invalid lines, open discrepancy, resolved discrepancy, and concurrency-conflict feedback. Assert no page-level overflow, editable raw row, clipped commands, or overlapping controls.

### Task 8: Run Regression, Builds, And Integrity Checks

**Files:**
- Modify only when a new failing regression proves a defect.

- [x] **Step 1: Run focused payment regression**

Use `-Plocal`, `MAVEN_OPTS=-Djdk.attach.allowAttachSelf=true -Xmx768m`, `-DforkCount=0`, and include:

```text
PaymentReconciliationPersistenceContractTest,
PaymentReconciliationCsvParserTest,
PaymentReconciliationServiceImplTest,
PaymentReconciliationMatcherTest,
PaymentReconciliationExecutionServiceTest,
PaymentReconciliationFailureRecorderTest,
PaymentReconciliationControllerContractTest,
PaymentReconciliationResolutionServiceTest,
PaymentSessionServiceImplTest,
PaymentWebhookServiceImplTest,
PaymentProviderAdminServiceImplTest,
PurchasePaymentEventServiceImplTest,
PurchaseReversalServiceImplTest,
PurchaseReversalReviewServiceImplTest
```

Expected: zero failures, errors, and skips.

- [x] **Step 2: Run wallet/member cross-module regression**

Include `WalletCoreServiceImplTest`, `WalletTurnoverTaskServiceImplTest`, `MemberProfileServiceImplTest`, and payment fulfillment/reversal suites. Expected: zero failures, errors, and skips.

- [x] **Step 3: Build all deliverables**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-admin -am '-DskipTests' package
$env:NODE_OPTIONS='--max-old-space-size=1792'; corepack pnpm --dir admin-ui build:dev
npm --prefix h5 run build
```

Temporarily stop only verified project-owned 8080/5173/5174 processes if Windows commit space or JAR locking requires it, then restore them.

- [x] **Step 4: Integrity scan**

Search reconciliation controller/services for wallet imports, payment mutation services, raw CSV logging, secret fields, missing tenant predicates, mutable line updates, unfinished markers, and cross-currency totals. Run `git diff --check`.

### Task 9: SQL Idempotency And Runtime Acceptance

**Files:**
- Modify after verification: `docs/superpowers/plans/2026-07-28-payment-reconciliation.md`
- Modify after verification: `progress.md`
- Modify after verification: `task_plan.md` using encoding-preserving byte replacement

- [x] **Step 1: Import SQL twice and verify exact schema**

Apply `gameluck_wallet.sql` twice to local `gameluck_vue`. Verify exactly four reconciliation tables, all tenant-first unique keys/indexes, one Admin page menu, five permissions, five dictionary types, exact dictionary values, and zero duplicate rows.

- [x] **Step 2: Validate an invalid-file batch**

Upload a CSV containing BOM/quoted values plus invalid decimal, unsupported event, duplicate Provider ID, and invalid timestamp rows. Confirm exact row errors/counts, batch `VALIDATED`, execute rejection, no issues, and no payment/wallet state changes.

- [x] **Step 3: Execute a corrected mixed batch**

Upload a valid file containing matched success/failure/cancel/refund/chargeback rows and fixtures for each issue type. Confirm deterministic primary issues, full diagnostics, matched/issue counts, UTC date-window behavior, and batch `COMPLETED`.

- [x] **Step 4: Verify digest and concurrency protection**

Re-upload identical bytes and confirm duplicate rejection. Send two execute requests against one fresh valid batch and confirm one success, one stable conflict, one issue set, and one completion action log.

- [x] **Step 5: Verify durable execution failure**

Use a controlled local persistence failure after acquiring `RECONCILING`. Confirm matching/issue writes roll back, batch persists `FAILED` with sanitized reason in `REQUIRES_NEW`, and no payment/wallet state changes occur.

- [x] **Step 6: Resolve and ignore representative issues**

Use real Admin permissions to resolve one issue and ignore another with mandatory remarks. Confirm terminal replay conflict, exact action logs, operator identity, and unchanged order/session/webhook/reversal/turnover/member-risk/wallet snapshots.

- [x] **Step 7: Browser acceptance**

Capture desktop and 390 px evidence for upload validation, invalid batch, completed summary, matched line, open issue, resolve dialog, resolved issue, and concurrency conflict. Verify related links, read-only raw/diagnostic content, disabled terminal commands, and no page-level overflow.

- [x] **Step 8: Restore services and run final checks**

Start backend with only `--spring.profiles.active=local` and restore Admin/H5 dev servers. Run the reconciliation frontend contract, targeted semantic ESLint, i18n, focused tests if any runtime fix was required, `git diff --check`, and verify HTTP 200 on 8080/5173/5174.

- [x] **Step 9: Record completion without committing**

Append exact test counts, builds, SQL counts, batch/issue IDs, digest/concurrency/failure evidence, screenshots, and service PIDs to `progress.md`. Mark every checkbox complete and change Phase 44 from `planned` to `completed` in `task_plan.md` without transcoding its mixed-encoding prefix. Do not run `git commit`.
