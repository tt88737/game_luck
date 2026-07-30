# Settlement Payout Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tenant-scoped maker-checker approval workflow that creates one internal payout instruction from one positive CLOSED settlement batch without transferring funds or mutating financial source state.

**Architecture:** Add an independent payout aggregate inside `gameluck-payment` with explicit tenant-scoped mappers, optimistic state transitions, and append-only action logs. The aggregate snapshots server-owned settlement facts, exposes permission-scoped Admin APIs, and uses the existing Vue/Element Plus operational patterns; it never extends the Phase 45 settlement state machine.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vite, Playwright.

---

## File Map

**Persistence and contracts**

- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementPayoutStatus.java`: exact five-state workflow enum.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementPayoutActionType.java`: exact workflow action enum.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementPayout.java`: payout instruction persistence model.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementPayoutActionLog.java`: append-only action model.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementPayoutMapper.java`: explicit reads, insert, edit, and guarded transitions.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementPayoutActionLogMapper.java`: insert and ordered history query.
- Modify `backend/script/sql/gameluck_wallet.sql`: two tables, page `2035`, permissions `20351`-`20356`, and Chinese dictionaries.
- Modify `backend/script/sql/gameluck_platform_dict.sql`: idempotent English payout dictionaries.

**Backend workflow**

- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementPayoutQueryBo.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementPayoutCreateBo.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementPayoutEditBo.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementPayoutCommandBo.java`.
- Create payout row, detail, and action VOs under `domain/vo`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementPayoutService.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementPayoutServiceImpl.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementPayoutApprovalService.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementPayoutController.java`.
- Modify the three backend i18n bundles with stable payout errors.

**Admin and verification**

- Create `admin-ui/src/api/payment/paymentSettlementPayout/types.ts` and `index.ts`.
- Create `admin-ui/src/views/payment/payment-settlement-payout/index.vue`.
- Modify Phase 45 and Phase 46 settlement views with eligible-batch entry points.
- Modify `admin-ui/src/lang/en_US.ts`, `admin-ui/src/lang/zh_CN.ts`, and `admin-ui/package.json`.
- Create `admin-ui/scripts/check-payment-settlement-payout-contract.mjs`.
- Create `admin-ui/scripts/phase47-payment-settlement-payout-runtime.mjs`.
- Create desktop/mobile evidence under `docs/implementation` and update `progress.md`.

### Task 1: Persist Payout Instructions And Metadata

**Files:** persistence enums/entities/mappers, wallet/platform SQL, backend messages, and `PaymentSettlementPayoutPersistenceContractTest.java`.

- [x] **Step 1: Write the failing persistence contract**

Assert exact statuses `DRAFT,PENDING_APPROVAL,APPROVED,REJECTED,CANCELLED`, actions `CREATE,EDIT,SUBMIT,APPROVE,REJECT,CANCEL`, money `BigDecimal`, version `Integer`, tenant-first unique keys, no sensitive fields, explicit mapper methods only, menu IDs `2035/20351-20356`, and stable message keys.

```java
assertThat(names(PaymentSettlementPayoutStatus.values())).containsExactly(
    "DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
assertThat(tableDefinition(sql, "gl_payment_settlement_payout")).contains(
    "payout_amount decimal(20,6) not null",
    "unique key uk_gl_payment_settlement_payout_02 (tenant_id, settlement_batch_id)");
```

- [x] **Step 2: Run RED**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-Dtest=PaymentSettlementPayoutPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: test compilation fails only because payout contracts do not exist.

- [x] **Step 3: Implement persistence and idempotent metadata**

Use `@TableName`, string-safe IDs in VOs only, explicit `@Insert/@Select/@Update`, and this guarded mapper surface:

```java
int insert(PaymentSettlementPayout entity);
PaymentSettlementPayout selectByTenantAndId(String tenantId, Long id);
PaymentSettlementPayout selectByTenantAndBatchId(String tenantId, Long batchId);
Page<PaymentSettlementPayout> selectPageByTenant(Page<?> page, String tenantId, String payoutNo,
    String settlementNo, String status, String providerCode, String currencyCode, Date start, Date end);
int editDraftOrRejected(String tenantId, Long id, int version, String purpose, String payeeReference,
    Date now);
int transition(String tenantId, Long id, int version, String expected, String next,
    Long operatorId, String operatorName, String reason, Date now);
```

- [x] **Step 4: Run GREEN and metadata scans**

Expected: persistence test passes; duplicate menu/dictionary values and sensitive-field scans return zero.

- [x] **Step 5: Commit and push module**

```powershell
git add backend/gameluck-modules/gameluck-payment/src backend/gameluck-admin/src/main/resources/i18n backend/script/sql
git commit -m "feat: persist settlement payout instructions"
git push
```

### Task 2: Create And Query Payout Instructions

**Files:** payout BOs/VOs, service interface/implementation, and `PaymentSettlementPayoutServiceImplTest.java`.

- [x] **Step 1: Write RED service tests**

Cover positive CLOSED creation, server-owned snapshot values, zero/negative/non-CLOSED/missing/cross-tenant rejection, duplicate creation, exact six-decimal serialization, filters, detail, and ordered actions.

```java
when(batchMapper.selectByTenantAndId("000000", 41L)).thenReturn(closed("12.340000"));
PaymentSettlementPayoutDetailVo result = service.create(createBo("41"));
assertThat(result.getPayoutAmount()).isEqualTo("12.340000");
verify(payoutMapper).insert(argThat(p -> p.getSettlementBatchId().equals(41L)
    && p.getProviderCode().equals("SIMULATED")));
```

- [x] **Step 2: Run RED**

Expected: fails on absent service and contracts.

- [x] **Step 3: Implement minimal create/query service**

Creation loads `PaymentSettlementBatchMapper.selectByTenantAndId`, requires `CLOSED` and `netSettlement.signum() > 0`, generates `PSP + snowflake`, validates trimmed purpose/reference, inserts `DRAFT` plus `CREATE` action in one transaction, and translates duplicate-key races to `payment.settlementPayout.duplicate`.

- [x] **Step 4: Run GREEN plus Phase 45 query regression**

Expected: payout service and `PaymentSettlementServiceImplTest` pass with zero failures/errors/skips.

- [x] **Step 5: Commit and push module**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementPayoutServiceImplTest.java
git commit -m "feat: create settlement payout instructions"
git push
```

### Task 3: Implement Edit, Submit, Cancel, And Resubmission

**Files:** payout service/mapper and `PaymentSettlementPayoutWorkflowTest.java`.

- [x] **Step 1: Write RED workflow tests**

Cover DRAFT edit/submit/cancel, REJECTED edit returning to DRAFT, resubmission, invalid transitions, stale versions, cross-tenant absence, one action per successful command, and no action on failure.

```java
service.edit(71L, editBo(3, "Revised purpose", "merchant-us"));
verify(mapper).editDraftOrRejected("000000", 71L, 3, "Revised purpose", "merchant-us", now);
verify(actionMapper).insert(argThat(log -> log.getActionType().equals("EDIT")
    && log.getAfterStatus().equals("DRAFT")));
```

- [x] **Step 2: Run RED; implement guarded commands; run GREEN**

Every command reloads after a zero-row update to distinguish not-found, invalid state, and stale version. Do not retry automatically.

- [x] **Step 3: Run transaction integration test**

Add `PaymentSettlementPayoutTransactionIntegrationTest` proving state and action rollback together.

- [x] **Step 4: Commit and push module**

```powershell
git add backend/gameluck-modules/gameluck-payment/src backend/gameluck-modules/gameluck-payment/src/test
git commit -m "feat: manage settlement payout workflow"
git push
```

### Task 4: Enforce Maker-Checker Approval

**Files:** approval service, payout service delegation, mapper transitions, and `PaymentSettlementPayoutApprovalServiceTest.java`.

- [x] **Step 1: Write RED approval tests**

Cover self-approval denial by maker ID, second-user approve/reject, APPROVED/CANCELLED terminal replay, REJECTED not directly approvable, reviewer identity persistence, sanitized evidence, and unchanged settlement mapper.

```java
when(operatorProvider.current()).thenReturn(new Operator(100L, "maker"));
assertThatThrownBy(() -> approval.approve("000000", pendingMadeBy(100L), command(2)))
    .hasMessage("payment.settlementPayout.selfApproval");
verify(payoutMapper, never()).transition(any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
```

- [x] **Step 2: Implement approval boundary**

Require `PENDING_APPROVAL`, reviewer ID different from `makerId`, required bounded reason, expected version, and atomic `APPROVE`/`REJECT` action insertion.

- [x] **Step 3: Run GREEN and financial dependency scan**

Expected: no wallet mapper, payment command, settlement update, reconciliation update, raw body, signature, credential, or bank field dependency.

- [x] **Step 4: Commit and push module**

```powershell
git add backend/gameluck-modules/gameluck-payment/src
git commit -m "feat: approve settlement payouts with maker checker"
git push
```

### Task 5: Expose Permission-Scoped Admin API

**Files:** controller, controller contract test, Admin typed API, API contract script, package script, and backend messages.

- [x] **Step 1: Write RED controller and frontend contracts**

Assert the eight exact routes, six permissions, sanitized `@Log` annotations, `TableDataInfo` list, string IDs/money, command version/reason fields, and `exposeBusinessCode` on state commands.

- [x] **Step 2: Implement controller and typed API**

```java
@SaCheckPermission("payment:settlementPayout:approve")
@PostMapping("/{id}/approve")
public R<PaymentSettlementPayoutDetailVo> approve(@PathVariable Long id,
        @Validated @RequestBody PaymentSettlementPayoutCommandBo bo) {
    return R.ok(service.approve(id, bo));
}
```

- [x] **Step 3: Run GREEN, `vue-tsc --noEmit`, and i18n**

Expected: controller test, API contract, full typecheck, and i18n exit `0`.

- [x] **Step 4: Commit and push module**

```powershell
git add backend/gameluck-modules/gameluck-payment/src backend/gameluck-admin/src/main/resources/i18n admin-ui/src/api/payment/paymentSettlementPayout admin-ui/scripts/check-payment-settlement-payout-contract.mjs admin-ui/package.json
git commit -m "feat: expose settlement payout operations"
git push
```

### Task 6: Build Settlement Payout Admin Workbench

**Files:** payout page, bilingual copy, Phase 45/46 entry points, and strengthened frontend contract.

- [ ] **Step 1: Extend RED frontend contract**

Assert filters, dense table, detail/action history, no-transfer warning, all state/permission commands, version conflict reload, eligible batch links, loading/error/empty/denied states, and mobile-local scrolling.

- [ ] **Step 2: Implement the page using Element Plus patterns**

Use one filter band, one table, one detail drawer, one create dialog, confirmation dialogs for submit/cancel, and reason dialogs for approve/reject. Never place cards inside cards or show a “paid” label.

- [ ] **Step 3: Add entry points**

Phase 45 shows `Create payout instruction` only for positive CLOSED batches. Phase 46 source-batch rows link to `/payment/payment-settlement-payout?settlementBatchId=<id>`; the destination revalidates eligibility through the backend.

- [ ] **Step 4: Run contracts, typecheck, i18n, and targeted ESLint**

Expected: all exit `0` with no semantic lint errors.

- [ ] **Step 5: Commit and push module**

```powershell
git add admin-ui/src/views/payment admin-ui/src/lang admin-ui/scripts/check-payment-settlement-payout-contract.mjs
git commit -m "feat: add settlement payout workbench"
git push
```

### Task 7: Run Regression And Production-Equivalent Builds

- [ ] **Step 1: Run focused payment regression**

Run payout, settlement, reconciliation, webhook, payment-event, and reversal tests with `-DforkCount=0`; record exact totals and zero failures/errors/skips.

- [ ] **Step 2: Run fresh-JVM cross-module regression**

Reuse Phase 46 wallet `19`, member `7`, and payment fulfillment/reversal `45` commands without reducing coverage.

- [ ] **Step 3: Build backend, Admin, and H5 with bounded memory**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -DskipTests package
$env:NODE_OPTIONS='--max-old-space-size=2048'; $env:ROLLUP_MAX_PARALLEL_FILE_OPS='1'
pnpm --dir admin-ui build:dev
pnpm --dir h5 build
```

- [ ] **Step 4: Run safety scans and record evidence in `progress.md`**

Expected: no financial source mutation dependency; `git diff --check` exits `0`.

- [ ] **Step 5: Commit and push module**

```powershell
git add progress.md
git commit -m "test: verify settlement payout regressions"
git push
```

### Task 8: Verify SQL And Runtime Approval Loop

**Files:** runtime script, desktop/mobile PNGs, checked plan, and `progress.md`.

- [ ] **Step 1: Import both SQL files twice and verify metadata**

Expect exactly two payout tables, one page, six permission children, two dictionary types, five status values, six action values, and no Phase 45/46 metadata changes.

- [ ] **Step 2: Start refreshed services and deterministic fixtures**

Use positive, zero, negative, non-CLOSED, and other-tenant settlement batches plus two Admin users with separated maker/reviewer permissions.

- [ ] **Step 3: Execute the real workflow**

Verify create, duplicate rejection, submit, self-approval denial, second-user approve, separate reject/edit/resubmit path, stale version, terminal replay, tenant isolation, exact action ordering, and absence of any “paid” claim.

- [ ] **Step 4: Prove read-only financial state**

Hash payment, reconciliation, settlement, member, turnover, and wallet source tables before/after. Only payout tables and sanitized Admin operation logs may change.

- [ ] **Step 5: Capture and inspect `1440x900` and `390x844` screenshots**

Require nonblank pixels, no console errors, no page overflow, readable warning, timeline, status, and commands.

- [ ] **Step 6: Run final verification**

Re-run payout backend tests, frontend contract, typecheck, i18n, targeted ESLint, safety scan, service health, and `git diff --check`.

- [ ] **Step 7: Mark plan complete and append recovery evidence**

Leave mixed-encoding `task_plan.md` untouched; append exact evidence and `Phase 47 completed` to `progress.md`.

- [ ] **Step 8: Commit and push module**

```powershell
git add admin-ui/scripts/phase47-payment-settlement-payout-runtime.mjs docs/implementation/phase47-payment-settlement-payout-*.png docs/superpowers/plans/2026-07-30-settlement-payout-approval.md progress.md
git commit -m "test: verify settlement payout runtime"
git push
```
