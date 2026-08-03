# Payment Settlement Instruction Aging Monitor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-configurable UTC natural-day SLAs and a read-only aging monitor that exposes due-soon, overdue, and stalled Phase 47 settlement instructions without changing instruction state.

**Architecture:** Persist one optimistic-lock aging policy per tenant, resolve system defaults when absent, and derive monitoring rows and full-filter summaries from active Phase 47 instruction fields at one injected-Clock evaluation instant. A dedicated mapper/service/controller boundary stays read-only toward instructions; the Admin workbench displays per-currency exposure and links into the existing Phase 47 workflow.

**Tech Stack:** Java 17, Spring Boot, MyBatis/MyBatis-Plus, MySQL 8, Sa-Token permissions, JUnit 5, AssertJ, Mockito, Vue 3, TypeScript, Element Plus, pnpm, Playwright.

---

## File Map

Backend policy and metadata:

- Create `PaymentSettlementAgingPolicy`, its update BO and effective-policy VO, mapper, service interface, and implementation.
- Create `PaymentSettlementAgingPersistenceContractTest` and `PaymentSettlementAgingPolicyServiceTest`.
- Modify `backend/script/sql/gameluck_wallet.sql` with one idempotent policy table, page `2036`, and permissions `20361`-`20363`.
- Modify all three backend i18n bundles with stable `payment.settlementAging.*` failures.

Backend read model:

- Create classification/bucket enums, monitor query BO, row/summary/currency/page VOs, `PaymentSettlementAgingMapper`, classifier, monitor service interface, and implementation.
- Reuse the existing UTC `Clock` bean; do not call `Instant.now()`, `LocalDate.now()`, or `new Date()` inside the monitor boundary.
- Read only `gl_payment_settlement_instruction`; do not update Phase 47 instructions or action logs.

Admin UI:

- Create `admin-ui/src/api/payment/paymentSettlementAging/{types.ts,index.ts}`.
- Create `admin-ui/src/views/payment/payment-settlement-aging/index.vue`.
- Create `admin-ui/scripts/check-payment-settlement-aging-contract.mjs` and register `check:payment-settlement-aging`.
- Modify the flat English and Chinese language files with all visible states.

Acceptance:

- Create `admin-ui/scripts/phase48-payment-settlement-aging-runtime.mjs`.
- Create desktop/mobile evidence PNGs and append exact results to `progress.md`.
- Do not rewrite mixed-encoding `task_plan.md`; this checked plan and `progress.md` are the recovery record.

### Task 1: Policy Persistence, Enums, Metadata, And Message Contracts

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementAgingPolicy.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementAgingClassification.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementAgingBucket.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingPolicyVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementAgingPersistenceContractTest.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`

- [ ] **Step 1: Write the failing persistence contract**

Require exact enum values, string-safe policy ID, SQL constraints, menu IDs, permissions, and message keys:

```java
assertThat(PaymentSettlementAgingClassification.values()).extracting(Enum::name)
    .containsExactly("NORMAL", "DUE_SOON", "OVERDUE", "STALLED");
assertThat(PaymentSettlementAgingBucket.values()).extracting(Enum::name)
    .containsExactly("DAYS_0_1", "DAYS_2_3", "DAYS_4_7", "DAYS_8_PLUS");
assertThat(PaymentSettlementAgingPolicyVo.class.getDeclaredField("id").getType()).isEqualTo(String.class);
assertThat(sql).contains(
    "CREATE TABLE IF NOT EXISTS gl_payment_settlement_aging_policy",
    "UNIQUE KEY uk_gl_payment_settlement_aging_policy_01 (tenant_id)",
    "(2036,'支付结算账龄监控'",
    "payment:settlementAging:list",
    "payment:settlementAging:query",
    "payment:settlementAging:policyEdit");
```

Require message keys for invalid policy, invalid filter, policy conflict, classification data integrity, and query failure in all three bundles.

- [ ] **Step 2: Run RED**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementAgingPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: test compilation fails only because the Phase 48 entity, enums, and VO do not exist.

- [ ] **Step 3: Add exact enums and policy contracts**

```java
public enum PaymentSettlementAgingClassification { NORMAL, DUE_SOON, OVERDUE, STALLED }
public enum PaymentSettlementAgingBucket { DAYS_0_1, DAYS_2_3, DAYS_4_7, DAYS_8_PLUS }
```

`PaymentSettlementAgingPolicy` uses internal `Long id`, `String tenantId`, six `Integer` day values, `Integer version`, and standard create/update actor/time fields. `PaymentSettlementAgingPolicyVo` exposes `id` as `String`, all day values and version as numbers, and `source` as `SYSTEM_DEFAULT` or `TENANT_OVERRIDE`.

- [ ] **Step 4: Add idempotent SQL and menu metadata**

```sql
CREATE TABLE IF NOT EXISTS gl_payment_settlement_aging_policy (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL,
  payable_execution_days INT NOT NULL,
  receivable_collection_days INT NOT NULL,
  draft_days INT NOT NULL,
  rejected_revision_days INT NOT NULL,
  pending_review_days INT NOT NULL,
  due_soon_days INT NOT NULL,
  version INT NOT NULL DEFAULT 0,
  create_by BIGINT NULL, create_time DATETIME NOT NULL,
  update_by BIGINT NULL, update_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_settlement_aging_policy_01 (tenant_id)
) ENGINE=InnoDB;
```

Allocate page `2036` at Payment Center order `10`, permissions `20361`-`20363`, and move sibling `19195` to order `11`. Do not add dictionaries: classifications and buckets are derived API enums with localized Admin copy, not configurable reference data.

- [ ] **Step 5: Add stable localized failures and run GREEN**

Add `payment.settlementAging.policy.invalid`, `.policy.conflict`, `.filter.invalid`, `.data.invalid`, and `.query.failed`. Run the Step 2 Maven command, then:

```powershell
rg -n "2036|2036[1-3]|payment:settlementAging" backend/script/sql/gameluck_wallet.sql
git diff --check
```

Expected: contract passes; exactly one page and three unique permission rows exist; whitespace check exits `0` apart from line-ending warnings.

- [ ] **Step 6: Commit**

```powershell
git add backend/script/sql/gameluck_wallet.sql backend/gameluck-admin/src/main/resources/i18n backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementAgingPolicy.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementAgingClassification.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementAgingBucket.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingPolicyVo.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementAgingPersistenceContractTest.java
git commit -m "feat: add settlement aging policy contracts"
```

### Task 2: Versioned Tenant Aging Policy Service

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementAgingPolicyUpdateBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementAgingPolicyMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementAgingPolicyService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementAgingPolicyServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementAgingPolicyServiceTest.java`

- [ ] **Step 1: Write RED policy tests**

Require:

```java
PaymentSettlementAgingPolicyVo effectivePolicy();
PaymentSettlementAgingPolicyVo update(PaymentSettlementAgingPolicyUpdateBo bo);
```

Test absent-row defaults `2/7/2/2/1/1`, tenant isolation, insert with `expectedVersion == null`, update with matching version, stale conflict, duplicate concurrent insert convergence to conflict, `1..90` SLA validation, `0..30` warning validation, and audit/version projection.

- [ ] **Step 2: Run RED**

Run the Task 1 Maven command with `-Dtest=PaymentSettlementAgingPolicyServiceTest`. Expected: compilation fails only on absent policy BO/mapper/service types.

- [ ] **Step 3: Implement tenant-scoped mapper and defaults**

```java
@Select("select * from gl_payment_settlement_aging_policy where tenant_id=#{tenantId} limit 1")
PaymentSettlementAgingPolicy selectByTenant(String tenantId);

@Update("update gl_payment_settlement_aging_policy set payable_execution_days=#{row.payableExecutionDays},"
    + "receivable_collection_days=#{row.receivableCollectionDays},draft_days=#{row.draftDays},"
    + "rejected_revision_days=#{row.rejectedRevisionDays},pending_review_days=#{row.pendingReviewDays},"
    + "due_soon_days=#{row.dueSoonDays},version=version+1,update_by=#{row.updateBy},update_time=#{row.updateTime} "
    + "where tenant_id=#{tenantId} and version=#{expectedVersion}")
int updateByTenantAndVersion(String tenantId, PaymentSettlementAgingPolicy row, Integer expectedVersion);
```

Keep defaults in one immutable factory; do not persist a row during reads.

- [ ] **Step 4: Implement validated optimistic updates**

Normalize and validate every field before persistence. Insert creates version `0`; update increments once. Catch only duplicate-key insertion races and return the stable policy conflict. Use `TenantHelper.getTenantId()`, `LoginHelper.getUserId()`, and the injected `Clock` for audit time.

- [ ] **Step 5: Run GREEN and mutation scan**

Run `PaymentSettlementAgingPolicyServiceTest,PaymentSettlementAgingPersistenceContractTest`. Scan the policy service and mapper for writes to any table except `gl_payment_settlement_aging_policy`. Expected: all tests pass and scan returns no forbidden mutations.

- [ ] **Step 6: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementAgingPolicyUpdateBo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementAgingPolicyMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementAgingPolicyService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementAgingPolicyServiceImpl.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementAgingPolicyServiceTest.java
git commit -m "feat: manage settlement aging policy"
```

### Task 3: UTC Classification And Tenant-Scoped Monitor Query

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementAgingQueryBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingRowVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingCurrencySummaryVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingSummaryVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAgingPageVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementAgingMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/aging/PaymentSettlementAgingClassifier.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementAgingService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementAgingServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/aging/PaymentSettlementAgingClassifierTest.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementAgingServiceImplTest.java`

- [ ] **Step 1: Write RED classifier tests**

Use `Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC)` and require:

```java
AgingResult classify(PaymentSettlementInstruction row, PaymentSettlementAgingPolicyVo policy, Instant evaluatedAt);
```

Cover `APPROVED` from `reviewedTime`, `OPEN` from `createTime`, `DRAFT` from `createTime`, `REJECTED` from `reviewedTime`, and `PENDING_REVIEW` from `submittedTime`. Assert August 1 plus two days is due through August 3 and overdue on August 4; test warning boundaries, all four buckets, priority, terminal exclusion, direction/status mismatch, and missing base timestamp.

- [ ] **Step 2: Run classifier RED, implement minimal classifier, and run GREEN**

Expected result contract:

```java
public record AgingResult(LocalDate baseDate, LocalDate deadlineDate, long ageDays,
    long remainingDays, long delayedDays, PaymentSettlementAgingBucket bucket,
    PaymentSettlementAgingClassification classification) {}
```

Use only `Instant -> LocalDate` with `ZoneOffset.UTC` and `ChronoUnit.DAYS`; do not use local timezone utilities. Run `PaymentSettlementAgingClassifierTest`; expected all cases pass.

- [ ] **Step 3: Write RED monitor service tests**

Require:

```java
PaymentSettlementAgingPageVo queryPage(PaymentSettlementAgingQueryBo bo, PageQuery pageQuery);
```

Test the default classifications `OVERDUE/STALLED/DUE_SOON`, explicit `NORMAL`, tenant/filter normalization, unsupported enums, Provider/currency normalization, string IDs/money, one captured `evaluatedAt`, paged rows, full-filter counts, per-currency amounts, empty results, terminal exclusion, and mapper failure localization.

- [ ] **Step 4: Add bounded tenant query and summary mapper**

The mapper must accept tenant, normalized filters, effective policy values, and one `evaluatedDate`. Its derived SQL uses the existing row timestamps:

```sql
CASE status
  WHEN 'APPROVED' THEN reviewed_time
  WHEN 'OPEN' THEN create_time
  WHEN 'DRAFT' THEN create_time
  WHEN 'REJECTED' THEN reviewed_time
  WHEN 'PENDING_REVIEW' THEN submitted_time
END AS aging_base_time
```

Use SQL `CASE` expressions for deadline/classification/bucket so filtering and pagination happen in the database. Exclude terminal statuses before derivation. Provide a paged row query plus one grouped summary query using identical predicates; do not select all active rows into Java memory.

- [ ] **Step 5: Implement monitor service and stable projections**

Capture `Instant evaluatedAt = clock.instant()` once, resolve the effective tenant policy once, normalize the query once, then call both mapper methods with `LocalDate.ofInstant(evaluatedAt, ZoneOffset.UTC)`. Set `evaluatedAt` as ISO text and include the effective policy in `PaymentSettlementAgingPageVo`. Convert all database `Long` IDs and `BigDecimal` values to plain strings.

- [ ] **Step 6: Run GREEN and read-only scans**

Run `PaymentSettlementAgingClassifierTest,PaymentSettlementAgingServiceImplTest,PaymentSettlementAgingPolicyServiceTest`. Then:

```powershell
rg -n "@(Insert|Update|Delete)|\b(insert|update|delete)\b" backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementAgingMapper.java
rg -n "PaymentSettlementInstruction(Service|Payable|Outcome)|IWallet|WalletMapper" backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementAgingServiceImpl.java
```

Expected: tests pass; both scans return no forbidden mutation or command dependency.

- [ ] **Step 7: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementAgingQueryBo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementAging*.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementAgingMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/aging backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementAgingService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementAgingServiceImpl.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/aging backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementAgingServiceImplTest.java
git commit -m "feat: query settlement instruction aging"
```

### Task 4: Permission-Scoped Admin API

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementAgingController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementAgingControllerContractTest.java`

- [ ] **Step 1: Write RED controller contract**

Assert `/payment/settlement-aging`, exact `GET /list`, `GET /policy`, and `PUT /policy` routes; exact list/query/policyEdit permissions; `BusinessType.UPDATE` and `isSaveRequestData=false/isSaveResponseData=false` on policy update; and dependencies only on the two Phase 48 service interfaces.

- [ ] **Step 2: Run RED**

Run the focused Maven command with `-Dtest=PaymentSettlementAgingControllerContractTest`. Expected: compilation fails because the controller is absent.

- [ ] **Step 3: Implement thin controller**

```java
@GetMapping("/list")
public PaymentSettlementAgingPageVo list(@Validated PaymentSettlementAgingQueryBo bo, PageQuery pageQuery)

@GetMapping("/policy")
public R<PaymentSettlementAgingPolicyVo> policy()

@PutMapping("/policy")
public R<PaymentSettlementAgingPolicyVo> updatePolicy(@Validated @RequestBody PaymentSettlementAgingPolicyUpdateBo bo)
```

Business validation remains in services. Policy mutation logging must not persist raw request or response payloads.

- [ ] **Step 4: Run GREEN and commit**

Run all `PaymentSettlementAging*Test` classes; expected zero failures/errors/skips.

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementAgingController.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementAgingControllerContractTest.java
git commit -m "feat: expose settlement aging monitor"
```

### Task 5: Typed Admin API And Responsive Monitoring Workbench

**Files:**
- Create: `admin-ui/src/api/payment/paymentSettlementAging/types.ts`
- Create: `admin-ui/src/api/payment/paymentSettlementAging/index.ts`
- Create: `admin-ui/src/views/payment/payment-settlement-aging/index.vue`
- Create: `admin-ui/scripts/check-payment-settlement-aging-contract.mjs`
- Modify: `admin-ui/package.json`
- Modify: `admin-ui/src/lang/en_US.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`

- [ ] **Step 1: Write the RED frontend contract**

Require the three exact endpoints and permissions, four classifications, four buckets, string IDs/money, numeric days/counts/versions, default abnormal classifications, no `Number()` money conversion, per-currency summaries, UTC evaluation text, policy conflict refresh, Phase 47 route navigation, all visible states, a two-by-two mobile summary grid, and table-local overflow. Register:

```json
"check:payment-settlement-aging": "node scripts/check-payment-settlement-aging-contract.mjs"
```

- [ ] **Step 2: Run RED**

```powershell
pnpm --dir admin-ui check:payment-settlement-aging
```

Expected: fails because the API and page do not exist.

- [ ] **Step 3: Add typed API contracts**

Define `SettlementAgingClassification`, `SettlementAgingBucket`, query, row, summary, currency summary, effective policy, page, and policy-update types. API functions are `listSettlementAging`, `getSettlementAgingPolicy`, and `updateSettlementAgingPolicy`. Keep ID/money fields as strings and policy/version/count/day fields numeric.

- [ ] **Step 4: Build the approved desktop workbench**

Implement the unframed work surface in this order: title/evaluated UTC time/SLA settings, four stable summary cells, aging distribution and per-currency exposure, normalized filters, and dense paged table. Rows show textual classification, instruction/source identifiers, direction, exact currency amount, Phase 47 status, age, deadline or delay, relevant actor evidence, and a permission-aware open-instruction command.

- [ ] **Step 5: Add policy editing and complete UI states**

The policy dialog displays system-default versus tenant-override source, validates five `1..90` SLA values and one `0..30` warning value, sends expected version, and refreshes on conflict. Implement loading, no active instructions, filtered empty, permission denied, query retry, policy-load failure, save progress/success/failure, validation failure, and conflict-refresh states. Query failures clear current summaries rather than presenting stale values as current.

- [ ] **Step 6: Add bilingual copy and responsive rules**

At `390px`, use a stable two-by-two count grid, retain readable currency rows, collapse secondary filters, keep the table's `overflow-x:auto` local, prevent page-level overflow, and tooltip long identifiers. Do not introduce assignment/owner copy or exception acknowledge/ignore controls.

- [ ] **Step 7: Run frontend GREEN and commit**

```powershell
pnpm --dir admin-ui check:payment-settlement-aging
pnpm --dir admin-ui check:payment-settlement-instruction
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui exec eslint src/views/payment/payment-settlement-aging/index.vue src/api/payment/paymentSettlementAging/index.ts src/api/payment/paymentSettlementAging/types.ts
git diff --check
```

Expected: all commands exit `0` apart from accepted line-ending warnings.

```powershell
git add admin-ui/package.json admin-ui/scripts/check-payment-settlement-aging-contract.mjs admin-ui/src/api/payment/paymentSettlementAging admin-ui/src/views/payment/payment-settlement-aging admin-ui/src/lang/en_US.ts admin-ui/src/lang/zh_CN.ts
git commit -m "feat: add settlement aging workbench"
```

### Task 6: Regression And Production-Equivalent Builds

**Files:**
- Modify only if an in-scope regression proves a defect.
- Modify: `progress.md`

- [ ] **Step 1: Run focused payment regression**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementAging*,PaymentSettlementInstruction*,PaymentSettlementReport*,PaymentSettlement*,PaymentReconciliation*,PurchaseReversalReview*' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: zero failures, errors, and skips; record the exact count.

- [ ] **Step 2: Run split cross-module regressions**

Reuse the verified fresh-JVM commands from Phase 47 for wallet `19/19`, member `7/7`, and payment fulfillment/reversal `45/45`. Expected: all pass without accumulating class metadata in one JVM.

- [ ] **Step 3: Build all deliverables with bounded memory**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -DskipTests package
$env:NODE_OPTIONS='--max-old-space-size=2048'
$env:ROLLUP_MAX_PARALLEL_FILE_OPS='1'
pnpm --dir admin-ui build:dev
pnpm --dir h5 build
```

Expected: backend `BUILD SUCCESS`; Admin and H5 exit `0`; only the established large-chunk advisory is acceptable.

- [ ] **Step 4: Run safety and consistency scans**

Scan Phase 48 production files for wallet/payment command dependencies, Phase 47 instruction writes, raw bodies, credentials, full account fields, filesystem writes, scheduled jobs, and non-policy SQL mutation. Run both aging/instruction frontend contracts, i18n, targeted ESLint, and `git diff --check`.

- [ ] **Step 5: Record and commit evidence**

Append exact test counts, build module counts, and warnings to `progress.md`:

```powershell
git add progress.md
git commit -m "test: verify settlement aging builds"
```

### Task 7: SQL, Runtime, UTC Boundary, And Responsive Acceptance

**Files:**
- Create: `admin-ui/scripts/phase48-payment-settlement-aging-runtime.mjs`
- Create: `docs/implementation/phase48-payment-settlement-aging-desktop.png`
- Create: `docs/implementation/phase48-payment-settlement-aging-mobile.png`
- Modify: `docs/superpowers/plans/2026-08-03-payment-settlement-instruction-aging-monitor.md`
- Modify: `progress.md`

- [ ] **Step 1: Import SQL twice and verify metadata**

Run `backend/script/bin/import-sql-utf8.ps1` twice. Assert exactly one policy table, page `2036`, three permissions `20361`-`20363`, no duplicate permissions, Phase 47 menu unchanged, and sibling `19195` at order `11`.

- [ ] **Step 2: Restart only verified project services**

Stop only listeners whose command lines point to the Phase 48 worktree on `8080/5173/5174`, start the refreshed backend with constrained JVM settings and the `local` profile, then Admin and H5. Expected: all three return HTTP `200`.

- [ ] **Step 3: Create deterministic tenant-isolated fixtures and source snapshots**

Create active Phase 47 instructions covering `DRAFT`, `REJECTED`, `PENDING_REVIEW`, `APPROVED`, and `OPEN`, plus every terminal status. Use fixed UTC timestamps around one evaluation date and another tenant containing a recognizable amount. Snapshot instruction/action-log, settlement/report, purchase/payment event, reconciliation, reversal, member-risk, turnover, wallet-account, and wallet-transaction tables.

- [ ] **Step 4: Verify defaults, overrides, UTC boundaries, and summaries**

With no tenant policy row, assert defaults `2/7/2/2/1/1` and exact `NORMAL/DUE_SOON/OVERDUE/STALLED` rows, buckets, delay days, and per-currency amounts. Save a tenant override, prove immediate reclassification without instruction writes, then test stale expected version. Run equivalent classifier tests at `23:59:59.999Z` and `00:00:00Z` around a deadline.

- [ ] **Step 5: Verify filters, pagination, tenant isolation, and source transitions**

Assert the default abnormal set excludes `NORMAL`, explicit normal filtering works, all status/direction/Provider/currency/bucket/number filters are stable, and summaries remain full-filter totals across pages. Prove the other tenant's recognizable amount never appears. Complete one source instruction through the real Phase 47 API and assert it disappears from the next monitor response without any Phase 48 exception command.

- [ ] **Step 6: Verify UI and immutable source state**

At `1440x900` and `390x844`, verify summary hierarchy, per-currency exposure, filters, exact money, textual states, policy source/edit/conflict, Phase 47 navigation gating, loading/empty/error states, two-by-two mobile summaries, table-local scrolling, no console errors, no page-level overflow, and nonblank pixels. Compare source checksums; only the policy table and permitted sanitized operation logs may change.

- [ ] **Step 7: Final verification and completion record**

Re-run all Phase 48 backend tests, aging/instruction frontend contracts, i18n, targeted ESLint, safety scans, runtime script, screenshot pixel checks, and `git diff --check`. Mark every checkbox in this plan and append exact runtime evidence plus `Phase 48 completed` to `progress.md`; leave `task_plan.md` untouched.

- [ ] **Step 8: Commit runtime evidence**

```powershell
git add admin-ui/scripts/phase48-payment-settlement-aging-runtime.mjs docs/implementation/phase48-payment-settlement-aging-desktop.png docs/implementation/phase48-payment-settlement-aging-mobile.png docs/superpowers/plans/2026-08-03-payment-settlement-instruction-aging-monitor.md progress.md
git commit -m "test: verify settlement aging runtime"
```
