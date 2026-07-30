# Payment Settlement Batch And Financial Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped settlement batches that snapshot processed simulated Provider events, calculate auditable gross/refund/chargeback/fee/net totals, and close only after Phase 44 reconciliation evidence is complete.

**Architecture:** Keep settlement inside `gameluck-payment`. A guarded calculation service reads processed webhook events and verified session/order money into immutable settlement items; a separate close service evaluates current reconciliation coverage and open issues without mutating any payment or wallet source. Admin APIs and one operational Vue page expose creation, calculation, evidence, and terminal close.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite, Playwright-compatible browser acceptance.

**Execution constraint:** Preserve the shared dirty `main` worktree. Do not create Git commits unless the user explicitly requests one; use review checkpoints instead of commit steps.

---

## File Map

- Persistence: settlement enums, three entities, and three mappers under `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment`, plus `backend/script/sql/gameluck_wallet.sql`.
- Calculation: `PaymentSettlementCalculator.java`, source records, calculation coordinator, and failure recorder under `service/settlement` and `service/impl`.
- Close gate: `PaymentSettlementReconciliationGate.java` and `PaymentSettlementCloseService.java` query Phase 44 evidence only.
- Admin boundary: `PaymentSettlementController.java`, BO/VO types, permissions, i18n, menu, and dictionaries.
- Admin UI: typed APIs under `admin-ui/src/api/payment/paymentSettlement/` and `admin-ui/src/views/payment/payment-settlement/index.vue`.
- Verification: focused backend tests, frontend contract script, SQL/runtime checks, browser evidence, `progress.md`, and `task_plan.md`.

### Task 1: Add Settlement Persistence Contracts

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementBatchStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementActionType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementBatch.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementItem.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementActionLog.java`
- Create: corresponding mapper interfaces under `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementPersistenceContractTest.java`

- [x] **Step 1: Write the failing persistence contract**

Assert exact states and actions:

```java
assertArrayEquals(new String[]{"CREATED", "CALCULATING", "CALCULATED", "CLOSED", "FAILED"},
    names(PaymentSettlementBatchStatus.values()));
assertArrayEquals(new String[]{"CREATE", "CALCULATE", "CALCULATION_FAILED", "CLOSE_REJECTED", "CLOSE"},
    names(PaymentSettlementActionType.values()));
```

Isolate all three SQL `CREATE TABLE` blocks. Assert tenant-first business keys, `(tenant_id, webhook_event_id)` uniqueness, `DECIMAL(20,6)` money, `DECIMAL(12,8)` rate, version, evidence/source JSON, and indexes for Provider/currency/window/status queries. Reject foreign keys, destructive DDL, raw bodies, signatures, and secret fields.

- [x] **Step 2: Run the persistence test and verify RED**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: compilation fails only because settlement types do not exist.

- [x] **Step 3: Implement exact entities, mappers, and guarded SQL**

Required mapper commands include:

```java
int transitionStatus(String tenantId, Long id, String expected, String next, Date now);
int completeCalculation(String tenantId, Long id, int expectedVersion, int eventCount,
    int paymentCount, int refundCount, int chargebackCount, BigDecimal grossPayment,
    BigDecimal refundAmount, BigDecimal chargebackAmount, BigDecimal totalFee,
    BigDecimal netSettlement, String evidenceJson, Date now);
int closeCalculated(String tenantId, Long id, int expectedVersion, Long operatorId,
    String operatorName, String remark, String evidenceJson, Date now);
boolean existsOverlapping(String tenantId, String providerCode, String currencyCode,
    Date periodStart, Date periodEnd, Long excludedId);
```

Use existing guarded `information_schema` patterns. No existing payment or wallet table is altered destructively.

- [x] **Step 4: Run Task 1 GREEN**

Run Step 2 unchanged. Expected: all persistence contract tests pass.

- [x] **Step 5: Review checkpoint**

Confirm every identifier lookup is tenant-scoped, source items are immutable, and no sensitive Provider payload is persisted.

### Task 2: Implement Settlement Creation And Query Projections

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementCreateBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementQueryBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementBatchVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementDetailVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementItemVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementActionLogVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementServiceImplTest.java`

- [x] **Step 1: Write failing creation/query tests**

Cover tenant scoping, supported Provider validation, uppercase ISO currency, UTC instant parsing, start-before-end, maximum 31 days, no future end, rate `0..1`, non-negative fixed fees, overlap rejection, deterministic settlement number, string-safe IDs/money, filtered pagination, item pagination, action order, and cross-tenant absence.

Desired API:

```java
PaymentSettlementDetailVo create(PaymentSettlementCreateBo bo);
TableDataInfo<PaymentSettlementBatchVo> queryPage(PaymentSettlementQueryBo bo, PageQuery pageQuery);
PaymentSettlementDetailVo queryDetail(Long batchId);
TableDataInfo<PaymentSettlementItemVo> queryItems(Long batchId, String eventType, PageQuery pageQuery);
```

- [x] **Step 2: Run tests and verify RED**

Run Task 1 command with `-Dtest=PaymentSettlementServiceImplTest`.

- [x] **Step 3: Implement minimal creation and read service**

Create a `CREATED` batch and append `CREATE` log in one transaction. Snapshot validated fee inputs without defaults hidden in Java. Return stable localized validation errors. Do not calculate during creation and do not expose source JSON in list responses.

- [x] **Step 4: Run Task 2 GREEN**

Expected: creation and tenant-scoped query tests pass.

- [x] **Step 5: Review checkpoint**

Verify half-open UTC intervals are used consistently and failed batches alone are excluded from overlap checks.

### Task 3: Build The Deterministic Financial Calculator

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/PaymentSettlementCalculator.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/SettlementSourceEvent.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/SettlementItemDraft.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/SettlementTotals.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentWebhookEventMapper.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/settlement/PaymentSettlementCalculatorTest.java`

- [x] **Step 1: Write failing formula and eligibility tests**

Cover successful payment fee rounding, refund with no fee, chargeback fixed fee, aggregate counts/totals, negative net, zero-rate/zero-fee, exclusion of failed/cancelled/unprocessed events, exact UTC start inclusion/end exclusion, currency isolation, stable ordering, and source snapshots without raw bodies.

Formula assertion example:

```java
SettlementTotals totals = calculator.calculate(events,
    new FeeRule(new BigDecimal("0.02900000"), new BigDecimal("0.300000"), new BigDecimal("15.000000")));
assertEquals(new BigDecimal("100.000000"), totals.grossPayment());
assertEquals(new BigDecimal("3.200000"), totals.totalFee());
assertEquals(new BigDecimal("96.800000"), totals.netSettlement());
```

- [x] **Step 2: Run calculator tests and verify RED**

Run Task 1 command with `-Dtest=PaymentSettlementCalculatorTest`.

- [x] **Step 3: Implement pure calculation and bounded source paging**

Use `BigDecimal.setScale(6, HALF_UP)`. Join source webhooks to sessions/orders in batched tenant-scoped queries; validate Provider, identity, currency, and equal money before producing drafts. Never deserialize money from `raw_body`.

- [x] **Step 4: Run Task 3 GREEN**

Expected: all eligibility, boundary, formula, and redaction cases pass.

- [x] **Step 5: Review checkpoint**

Confirm only three processed financial event types enter the result and every event produces exactly one signed net contribution.

### Task 4: Coordinate Atomic Calculation And Durable Failure

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementCalculationService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementFailureRecorder.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementCalculationServiceTest.java`

- [x] **Step 1: Write failing transaction/concurrency tests**

Cover guarded ownership, item batching, totals matching item sums, zero eligible events rejection, duplicate webhook protection, concurrent callers, item-insert rollback, total-update rollback, sanitized `FAILED` persistence through a separate recorder, and no partial items after failure.

- [x] **Step 2: Run tests and verify RED**

Run Task 1 command with `-Dtest=PaymentSettlementCalculationServiceTest`.

- [x] **Step 3: Implement calculation coordination**

Guard `CREATED -> CALCULATING`, calculate and insert items in one transaction, then guard `CALCULATING -> CALCULATED`. Append `CALCULATE` only after success. On owned infrastructure/source-integrity failure, invoke a separate Spring bean with `REQUIRES_NEW` to store `FAILED` and `CALCULATION_FAILED` after rollback.

- [x] **Step 4: Run Task 4 GREEN**

Expected: transaction and concurrency tests pass with no partial side effects.

- [x] **Step 5: Review checkpoint**

Confirm retries cannot duplicate source webhooks and failure messages expose no SQL, payload, stack trace, or path.

### Task 5: Enforce Reconciliation Evidence At Close

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/PaymentSettlementReconciliationGate.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/settlement/SettlementReconciliationEvidence.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementCloseBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementCloseService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentReconciliationBatchMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentReconciliationIssueMapper.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementCloseServiceTest.java`

- [x] **Step 1: Write failing evidence and close tests**

Cover every touched UTC date, missing date coverage, same Provider/tenant only, currency-specific open issues, resolved/ignored non-blocking counts, nonblank close remark, stale version, non-calculated batch, concurrent close, terminal replay, exact evidence JSON, close rejection log, close success log, and no reconciliation mutation.

- [x] **Step 2: Run tests and verify RED**

Run Task 1 command with `-Dtest=PaymentSettlementCloseServiceTest`.

- [x] **Step 3: Implement current-time evidence evaluation and guarded close**

Query `COMPLETED` reconciliation batches for all dates touched by the half-open interval. Count `OPEN` issues where either Provider or platform currency equals the batch currency. Store completed batch IDs, dates, missing dates, and status counts in canonical JSON. On success, guard `CALCULATED -> CLOSED` with version and append the required remark. On expected rejection, append only `CLOSE_REJECTED`.

- [x] **Step 4: Run Task 5 GREEN**

Expected: all coverage, issue-state, concurrency, and immutability tests pass.

- [x] **Step 5: Review checkpoint**

Verify evidence is recalculated at close and no cached count can bypass an issue opened before close.

### Task 6: Add Admin API, Permissions, SQL Metadata, And I18n

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementController.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementControllerContractTest.java`

- [x] **Step 1: Write failing controller/metadata contracts**

Assert the six approved routes, five exact permissions, `@Log` on create/calculate/close, request validation, string-safe serialization, menu placement after reconciliation, bilingual menu/dictionary values, and idempotent SQL guards.

- [x] **Step 2: Run contracts and verify RED**

Run Task 1 command with `-Dtest=PaymentSettlementControllerContractTest`.

- [x] **Step 3: Implement controller and metadata**

Use `@SaCheckPermission` per command. Create and close request bodies must be `@Validated`. Suppress sensitive request/response logging where remarks or evidence could be copied. Add stable localized messages for overlap, invalid window/rate, calculation conflict/failure, missing reconciliation dates, open issues, and terminal close.

- [x] **Step 4: Run Task 6 GREEN**

Expected: route, permission, SQL, and i18n contracts pass.

- [x] **Step 5: Review checkpoint**

Confirm query permission cannot calculate or close and SQL can be imported repeatedly.

### Task 7: Build Typed Admin Settlement Workbench

**Files:**
- Create: `admin-ui/src/api/payment/paymentSettlement/index.ts`
- Create: `admin-ui/src/api/payment/paymentSettlement/types.ts`
- Create: `admin-ui/src/views/payment/payment-settlement/index.vue`
- Modify: `admin-ui/src/lang/en_US.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Create: `admin-ui/scripts/check-payment-settlement-contract.mjs`
- Modify: `admin-ui/package.json`

- [x] **Step 1: Write the failing frontend contract**

Assert all API routes and permissions, identifiers/money/rates as strings, exact status/action mappings, UTC form fields, 31-day validation, fee validation, detail tabs, event filters, evidence links, required close remark, terminal-state command hiding, and no mutation API for items or totals.

- [x] **Step 2: Run contract and verify RED**

```powershell
pnpm --dir admin-ui check:payment-settlement
```

Expected: failure because API types and page do not exist.

- [x] **Step 3: Implement the operational page**

Use the existing payment tables/drawers and `payment-reconciliation` navigation patterns. Display signed net values without color-only meaning. Use a percentage input backed by decimal rate, currency swatches, icon commands with tooltips, required confirmations, local table scrolling, and a one-column 390 px detail layout. Do not place cards inside cards.

- [x] **Step 4: Run focused frontend checks**

```powershell
pnpm --dir admin-ui check:payment-settlement
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui exec eslint src/views/payment/payment-settlement/index.vue src/api/payment/paymentSettlement/index.ts src/api/payment/paymentSettlement/types.ts
```

Expected: all commands exit 0.

- [x] **Step 5: Review checkpoint**

Confirm long identifiers and negative totals remain readable, commands do not shift layout, and close copy states that source payment/wallet data is unchanged.

### Task 8: Run Backend Regression And Production-Equivalent Builds

**Files:**
- Modify only when a failing regression proves an in-scope defect.

- [x] **Step 1: Run the complete focused settlement suite**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlement*,PaymentReconciliation*,PaymentWebhook*,PurchasePaymentEvent*,PurchaseReversal*' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: zero failures, errors, and skips.

- [x] **Step 2: Run wallet/member/payment cross-module regression**

Use explicit `-DskipTests=false`, `-Plocal`, `-DforkCount=0`, and the established Phase 44 cross-module class list. Expected: zero failures and errors.

- [x] **Step 3: Build backend and both frontends**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -DskipTests package
$env:NODE_OPTIONS='--max-old-space-size=1792'
pnpm --dir admin-ui build:dev
pnpm --dir h5 build
```

Expected: backend `BUILD SUCCESS`; Admin and H5 builds exit 0.

- [x] **Step 4: Run static safety checks**

```powershell
rg -n "wallet|IWallet|Wallet.*Mapper|rawBody|signature" backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlement* backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementController.java
git diff --check
```

Expected: no wallet dependency or sensitive payload use in settlement code; diff check exits 0 apart from existing line-ending warnings.

- [x] **Step 5: Review checkpoint**

Record exact test/build counts and any pre-existing warnings in `progress.md`.

### Task 9: Verify SQL Idempotency And Real Runtime Behavior

**Files:**
- Create: `admin-ui/scripts/phase45-payment-settlement-runtime.mjs`
- Create evidence: `docs/implementation/phase45-payment-settlement-runtime-desktop.png`
- Create evidence: `docs/implementation/phase45-payment-settlement-runtime-mobile.png`
- Modify: `progress.md`
- Modify: `task_plan.md`

- [x] **Step 1: Import SQL twice and inspect exact counts**

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_platform_dict.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_platform_dict.sql
```

Query `information_schema`, `sys_menu`, `sys_dict_type`, and `sys_dict_data`. Expected: one set of three tables, one page, five permissions, unique dictionary rows, and no duplicates.

- [x] **Step 2: Restart the refreshed backend and verify three services**

Start the packaged backend only with `--spring.profiles.active=local`. Reuse free ports for Admin/H5. Expected: backend `8080`, Admin `5173`, and H5 `5174` return HTTP 200.

- [x] **Step 3: Create deterministic financial source events**

Through the actual H5/simulated checkout flow, create processed payment success, refund success, and chargeback events in one currency. Snapshot related orders, sessions, webhooks, payment events, reversals, member risk, turnover, wallet accounts, and wallet transactions before settlement commands.

- [x] **Step 4: Calculate and exercise both close blockers**

Create a settlement batch with known fee values and verify every item plus the exact formula. First close with a missing UTC reconciliation date and expect rejection. Import/execute the required Phase 44 statement with one open issue and expect the second rejection. Resolve or ignore it through the real reconciliation UI/API.

- [x] **Step 5: Close, replay, and prove read-only behavior**

Close with a nonblank remark, replay close and expect the stable terminal conflict, then compare all source snapshots. Expected: settlement/evidence/action rows changed; payment, reversal, risk, turnover, and wallet snapshots did not.

- [x] **Step 6: Capture desktop and mobile browser evidence**

Use the actual encrypted browser login flow. At `1440x900` and `390x844`, verify list, create form, calculated details, blocker evidence, closed state, tooltips, dialogs, no console errors, no page-level overflow, and nonblank canvas/page pixels. Save the two named screenshots.

- [x] **Step 7: Final consistency review**

Re-run focused contracts and `git diff --check`; mark every plan checkbox, append exact evidence to `progress.md`, and add Phase 45 as `completed` in `task_plan.md`. Preserve its historical byte encoding and do not commit.
