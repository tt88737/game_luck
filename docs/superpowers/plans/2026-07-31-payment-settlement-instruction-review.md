# Payment Settlement Instruction And Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one tenant-scoped, auditable payable/receivable/no-action instruction per closed settlement batch, with creator-separated single review and manually evidenced external outcomes.

**Architecture:** Add a dedicated instruction aggregate, append-only action log, mapper, service boundary, state-transition services, and Admin controller inside `gameluck-payment`. Phase 47 reads immutable Phase 45 batches but never invokes settlement, payment, reconciliation, member-risk, turnover, or wallet command services. A typed Admin workbench uses one dense list and a detail drawer; every command carries `expectedVersion` and `requestKey` so optimistic locking and replay behavior are explicit.

**Tech Stack:** Java 17, Spring Boot, MyBatis/MyBatis-Plus, MySQL 8, Sa-Token permissions, JUnit 5, AssertJ, Mockito, Vue 3, TypeScript, Element Plus, pnpm, Playwright.

---

## File Map

Backend persistence and contracts:

- Create `PaymentSettlementInstruction`, `PaymentSettlementInstructionActionLog`, three enums, command/query BOs, list/detail/action VOs, and two mappers under the existing `com.gameluck.payment` packages.
- Create `PaymentSettlementInstructionPersistenceContractTest` to lock schema, indexes, enums, menu metadata, message keys, and string-safe projections.
- Modify `backend/script/sql/gameluck_wallet.sql` with two idempotent tables, dictionary metadata, page `2035`, and permissions `20351` through `20359`.
- Modify all three backend i18n bundles with stable `payment.settlementInstruction.*` failures.

Backend behavior:

- Create `IPaymentSettlementInstructionService` and `PaymentSettlementInstructionServiceImpl` for query, detail, log projection, and idempotent generation.
- Create `PaymentSettlementInstructionPayableService` for edit, submit, approve, reject, and cancel.
- Create `PaymentSettlementInstructionOutcomeService` for execute, collect, and waive.
- Create `PaymentSettlementInstructionOperatorProvider` so actor lookup is isolated and mockable.
- Create focused service tests plus transaction integration tests for rollback/concurrency boundaries.
- Create `PaymentSettlementInstructionController` and a reflection contract test for exact routes, permissions, and sanitized operation logging.

Admin UI:

- Create `admin-ui/src/api/payment/paymentSettlementInstruction/{types.ts,index.ts}`.
- Create `admin-ui/src/views/payment/payment-settlement-instruction/index.vue`.
- Create `admin-ui/scripts/check-payment-settlement-instruction-contract.mjs` and add `check:payment-settlement-instruction` to `admin-ui/package.json`.
- Modify Phase 45 and Phase 46 pages only to add instruction drill-down/generation navigation; retain their existing behavior.
- Modify `admin-ui/src/lang/en_US.ts` and `admin-ui/src/lang/zh_CN.ts` with complete visible-state copy.

Acceptance:

- Create `admin-ui/scripts/phase47-payment-settlement-instruction-runtime.mjs`.
- Create desktop/mobile evidence PNGs and append exact results to `progress.md`.
- Do not rewrite mixed-encoding `task_plan.md`; the checked implementation plan and `progress.md` are the recovery record.

### Task 1: Persistence, Enum, Metadata, And Message Contracts

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementInstructionDirection.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementInstructionStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSettlementInstructionActionType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementInstruction.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementInstructionActionLog.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionActionLogVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementInstructionPersistenceContractTest.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`

- [ ] **Step 1: Write the failing persistence contract**

Add a reflection/SQL contract that requires exact enums and tables:

```java
assertThat(PaymentSettlementInstructionDirection.values())
    .extracting(Enum::name).containsExactly("PAYABLE", "RECEIVABLE", "BALANCED");
assertThat(PaymentSettlementInstructionStatus.values()).extracting(Enum::name)
    .containsExactly("DRAFT", "PENDING_REVIEW", "REJECTED", "APPROVED", "EXECUTED",
        "CANCELLED", "OPEN", "COLLECTED", "WAIVED", "NO_ACTION");
assertThat(sql).contains(
    "CREATE TABLE IF NOT EXISTS gl_payment_settlement_instruction",
    "UNIQUE KEY uk_gl_payment_settlement_instruction_01 (tenant_id, settlement_batch_id)",
    "CREATE TABLE IF NOT EXISTS gl_payment_settlement_instruction_action_log",
    "UNIQUE KEY uk_gl_payment_settlement_instruction_action_01 (tenant_id, instruction_id, request_key)",
    "(2035,'支付结算指令',1900,9,'payment-settlement-instruction'",
    "payment:settlementInstruction:waive");
assertThat(PaymentSettlementInstructionVo.class.getDeclaredField("amount").getType())
    .isEqualTo(String.class);
```

Also assert action types in this exact order: `CREATE`, `EDIT`, `SUBMIT`, `REJECT`, `APPROVE`, `CANCEL`, `EXECUTE`, `COLLECT`, `WAIVE`, and assert every stable message key exists in all three bundles.

- [ ] **Step 2: Run RED**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementInstructionPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: test compilation fails only because the new enums/entities/VOs do not exist.

- [ ] **Step 3: Add exact enums and entities**

Use plain enums and MyBatis entities matching existing settlement style:

```java
public enum PaymentSettlementInstructionDirection { PAYABLE, RECEIVABLE, BALANCED }

public enum PaymentSettlementInstructionStatus {
    DRAFT, PENDING_REVIEW, REJECTED, APPROVED, EXECUTED,
    CANCELLED, OPEN, COLLECTED, WAIVED, NO_ACTION
}

public enum PaymentSettlementInstructionActionType {
    CREATE, EDIT, SUBMIT, REJECT, APPROVE, CANCEL, EXECUTE, COLLECT, WAIVE
}
```

`PaymentSettlementInstruction` must contain the exact source snapshot, counterparty, actor/time, status, version, and audit fields from the design. Use `Long` internally, `BigDecimal` for signed net/absolute amount, `Date` for UTC instants, and `@TableName("gl_payment_settlement_instruction")`. The action entity contains before/after status/direction/version, request key, payload digest, sanitized evidence JSON, operator, and create time.

- [ ] **Step 4: Add idempotent SQL and metadata**

Add tables with `decimal(20,6)`, explicit tenant indexes, a foreign-key-free operational schema consistent with existing SQL, and these keys:

```sql
UNIQUE KEY uk_gl_payment_settlement_instruction_01 (tenant_id, settlement_batch_id),
UNIQUE KEY uk_gl_payment_settlement_instruction_02 (tenant_id, instruction_no),
KEY idx_gl_payment_settlement_instruction_01 (tenant_id, direction, status, create_time, id),
KEY idx_gl_payment_settlement_instruction_02 (tenant_id, provider_code, currency_code, create_time, id)
```

Allocate page `2035`, permissions `20351`-`20359`, dictionary types `gl_payment_settlement_instruction_direction/status/action`, dictionary type IDs `20047`-`20049`, and dictionary data IDs `21327`-`21348`. Use the nine exact permission literals from the design. Insert page `2035` at Payment Center order `9` and move existing later sibling `19195` to order `10`.

- [ ] **Step 5: Add stable localized failures**

Add keys for not-found, source-not-closed, duplicate, source-invalid, action-invalid, state-conflict, self-review, request-required, request-conflict, version-required, evidence-required, sensitive-destination, and event-time-invalid. English text must be direct; default and `zh_CN` use escaped Chinese consistent with existing bundles.

- [ ] **Step 6: Run GREEN and metadata scans**

Run the Step 2 Maven command, then:

```powershell
rg -n "2035|2035[1-9]|payment:settlementInstruction" backend/script/sql/gameluck_wallet.sql
git diff --check
```

Expected: contract passes; exactly one page and nine unique permission rows are present; whitespace check exits `0` apart from line-ending warnings.

- [ ] **Step 7: Commit**

```powershell
git add backend/script/sql/gameluck_wallet.sql backend/gameluck-admin/src/main/resources/i18n backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementInstruction.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSettlementInstructionActionLog.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionVo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionActionLogVo.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementInstructionPersistenceContractTest.java
git commit -m "feat: add settlement instruction contracts"
```

### Task 2: Tenant-Scoped Query And Idempotent Generation

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionQueryBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionGenerateBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionDetailVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionActionLogMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementInstructionService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionOperatorProvider.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImplTest.java`

- [ ] **Step 1: Write RED service tests**

Cover tenant filters, string-safe projections, source state, direction derivation, initial states, exact six-decimal values, request replay, changed-payload conflict, duplicate batch mapping, absent/cross-tenant batches, and operator absence. Use these interface signatures:

```java
TableDataInfo<PaymentSettlementInstructionVo> queryPage(PaymentSettlementInstructionQueryBo bo, PageQuery pageQuery);
PaymentSettlementInstructionDetailVo queryDetail(Long instructionId);
List<PaymentSettlementInstructionActionLogVo> queryActions(Long instructionId);
PaymentSettlementInstructionDetailVo generate(Long batchId, PaymentSettlementInstructionGenerateBo bo);
```

Generation tests must assert `12.340000 -> PAYABLE/DRAFT/12.340000`, `-8.500000 -> RECEIVABLE/OPEN/8.500000`, and `0.000000 -> BALANCED/NO_ACTION/0.000000`.

- [ ] **Step 2: Run RED**

Run the focused Maven command from Task 1 with `-Dtest=PaymentSettlementInstructionServiceImplTest`. Expected: compilation fails only on the absent Phase 47 mapper/service/contracts.

- [ ] **Step 3: Implement query and generation contracts**

`PaymentSettlementInstructionGenerateBo` contains validated `requestKey`. Query BO contains instruction/settlement number, direction, status, Provider, currency, and begin/end create times. The operator provider exposes:

```java
public record InstructionOperator(Long id, String name) {}
public InstructionOperator current() {
    Long id = LoginHelper.getUserId();
    if (id == null) throw new ServiceException(MessageUtils.message("payment.settlementInstruction.operator.required"));
    return new InstructionOperator(id, StringUtils.blankToDefault(LoginHelper.getUsername(), "unknown"));
}
```

The mapper must include tenant ID in every select/update and provide paged query, tenant/id detail, tenant/batch lookup, action lookup by request key, ordered action list, insert, and optimistic update methods.

- [ ] **Step 4: Implement atomic idempotent generation**

Inside `@Transactional(rollbackFor = Exception.class)`, normalize the request key, compute a stable payload digest from action type + batch ID, return the existing result only when action and digest match, load the batch through `PaymentSettlementBatchMapper.selectByTenantAndId`, require `CLOSED`, derive direction/amount/status, insert the instruction, and append `CREATE`. Convert duplicate-key generation to the existing instruction for the same tenant/batch; never expose another tenant's row.

- [ ] **Step 5: Run GREEN and read-only dependency scan**

Run Task 1 and Task 2 tests together. Then:

```powershell
rg -n "IWallet|Wallet.*Mapper|Payment.*Command|Reconciliation.*Service|rawBody|signature" backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImpl.java
```

Expected: tests pass; scan has no forbidden dependency.

- [ ] **Step 6: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstruction* backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementInstructionDetailVo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstruction* backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementInstructionService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstruction* backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImplTest.java
git commit -m "feat: generate settlement instructions"
```

### Task 3: Payable Edit, Submit, Review, And Cancel State Machine

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionEditBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionCommandBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionPayableService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionPayableServiceTest.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionTransactionIntegrationTest.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementInstructionService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImpl.java`
- Modify: both Phase 47 mappers from Task 2

- [ ] **Step 1: Write RED payable state tests**

Require methods:

```java
PaymentSettlementInstructionDetailVo edit(Long id, PaymentSettlementInstructionEditBo bo);
PaymentSettlementInstructionDetailVo submit(Long id, PaymentSettlementInstructionCommandBo bo);
PaymentSettlementInstructionDetailVo approve(Long id, PaymentSettlementInstructionCommandBo bo);
PaymentSettlementInstructionDetailVo reject(Long id, PaymentSettlementInstructionCommandBo bo);
PaymentSettlementInstructionDetailVo cancel(Long id, PaymentSettlementInstructionCommandBo bo);
```

`EditBo` contains `expectedVersion`, `requestKey`, counterparty name, external method, masked destination reference, and remark. `CommandBo` contains `expectedVersion`, `requestKey`, and remark. Tests cover exact allowed transitions, `REJECTED -> edit -> PENDING_REVIEW`, mandatory review/cancel reasons, non-payable rejection, self-review denial, stale version, terminal protection, replay, changed-payload conflict, and exactly one action log per success.

- [ ] **Step 2: Run RED**

Run `-Dtest=PaymentSettlementInstructionPayableServiceTest,PaymentSettlementInstructionTransactionIntegrationTest`. Expected: compilation fails only because the payable service and commands are absent.

- [ ] **Step 3: Implement bounded edit and transition helpers**

Reject unmasked account-like destination values; accept only a bounded masked reference such as `Account ending 4821`. Trim counterparty/method to 128 characters, destination to 128, and remark to 500. Centralize request replay validation and optimistic update result checking; do not duplicate these rules across command methods.

- [ ] **Step 4: Implement payable workflow**

Only `PAYABLE` is eligible. Edit accepts `DRAFT/REJECTED`; submit requires all counterparty fields; approve/reject require `PENDING_REVIEW`; reviewer ID must differ from creator ID; cancel accepts `DRAFT`, `REJECTED`, `PENDING_REVIEW`, or `APPROVED`. Every update and action insert occur in one transaction and increment version exactly once.

- [ ] **Step 5: Verify rollback and concurrency**

The integration test must force action-log insertion failure after a transition and assert the instruction rolls back, then run two commands at one expected version and assert exactly one succeeds. Run all Phase 47 backend tests created so far; expected zero failures/errors/skips.

- [ ] **Step 6: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionEditBo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionCommandBo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementInstructionService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionPayableService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImpl.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionActionLogMapper.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionPayableServiceTest.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionTransactionIntegrationTest.java
git commit -m "feat: review payable settlement instructions"
```

### Task 4: External Execution, Collection, And Waiver Outcomes

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionOutcomeBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionOutcomeService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionOutcomeServiceTest.java`
- Modify: `IPaymentSettlementInstructionService`, `PaymentSettlementInstructionServiceImpl`, and both instruction mappers

- [ ] **Step 1: Write RED outcome tests**

Require `execute`, `collect`, and `waive` methods. `OutcomeBo` contains `expectedVersion`, `requestKey`, `externalReference`, `eventTime`, and `remark`. Cover `APPROVED PAYABLE -> EXECUTED`, `OPEN RECEIVABLE -> COLLECTED/WAIVED`, wrong-direction denial, mandatory references, mandatory waiver reason, event time lower bound, future tolerance of five minutes, replay, conflict, and terminal immutability.

- [ ] **Step 2: Run RED**

Run `-Dtest=PaymentSettlementInstructionOutcomeServiceTest`. Expected: compilation fails on the missing outcome BO/service methods.

- [ ] **Step 3: Implement exact outcome validation**

External references are trimmed to 128 characters and remarks to 500. Execute time must be at or after approval time; collection time must be at or after instruction creation; both may be no more than five minutes ahead of server UTC. Waive ignores external reference/time and requires a nonblank reason.

- [ ] **Step 4: Implement transactional terminal transitions**

Use the same digest/replay/expected-version helpers as Task 3. Persist only sanitized external evidence, actor, time, terminal status, and one append-only action. Do not create files or invoke external APIs.

- [ ] **Step 5: Run GREEN and forbidden mutation scan**

Run all `PaymentSettlementInstruction*Test` classes. Then scan Phase 47 services/mappers for wallet dependencies and SQL mutations outside the two new tables. Expected: tests pass and no forbidden matches.

- [ ] **Step 6: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementInstructionOutcomeBo.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementInstructionService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionOutcomeService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionServiceImpl.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementInstructionActionLogMapper.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementInstructionOutcomeServiceTest.java
git commit -m "feat: close settlement instruction outcomes"
```

### Task 5: Permission-Scoped Admin API

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementInstructionController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementInstructionControllerContractTest.java`

- [ ] **Step 1: Write RED controller contract**

Assert `/payment/settlement-instruction`, the twelve exact endpoints from the design, nine exact permissions, `BusinessType.INSERT` for generation, `BusinessType.UPDATE` for commands, and `isSaveRequestData=false/isSaveResponseData=false` on every mutation log. Assert the controller depends only on `IPaymentSettlementInstructionService`.

- [ ] **Step 2: Run RED**

Run `-Dtest=PaymentSettlementInstructionControllerContractTest`. Expected: compilation fails because the controller is absent.

- [ ] **Step 3: Implement the thin controller**

Map list/detail/actions plus generation, edit, submit, approve, reject, cancel, execute, collect, and waive. Use `R.ok(...)`, `TableDataInfo`, `@Validated`, `@SaCheckPermission`, and sanitized `@Log` annotations. Business decisions remain in services.

- [ ] **Step 4: Run GREEN with service regressions**

Run all `PaymentSettlementInstruction*Test` classes. Expected: all controller and service tests pass with zero failures/errors/skips.

- [ ] **Step 5: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementInstructionController.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementInstructionControllerContractTest.java
git commit -m "feat: expose settlement instruction operations"
```

### Task 6: Typed Admin API And Unified Workbench

**Files:**
- Create: `admin-ui/src/api/payment/paymentSettlementInstruction/types.ts`
- Create: `admin-ui/src/api/payment/paymentSettlementInstruction/index.ts`
- Create: `admin-ui/src/views/payment/payment-settlement-instruction/index.vue`
- Create: `admin-ui/scripts/check-payment-settlement-instruction-contract.mjs`
- Modify: `admin-ui/package.json`
- Modify: `admin-ui/src/views/payment/payment-settlement/index.vue`
- Modify: `admin-ui/src/views/payment/payment-settlement-report/index.vue`
- Modify: `admin-ui/src/lang/en_US.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`

- [ ] **Step 1: Write the RED frontend contract**

The contract must require the exact base URL and twelve functions, nine permission constants, string IDs/money, numeric versions/counts only, all three directions and ten statuses, no `Number()` conversion of money, all visible states, source drill-down, creator self-review gating, and responsive local table scrolling. Add `"check:payment-settlement-instruction": "node scripts/check-payment-settlement-instruction-contract.mjs"`.

- [ ] **Step 2: Run RED**

```powershell
pnpm --dir admin-ui check:payment-settlement-instruction
```

Expected: fails because the API and page are absent.

- [ ] **Step 3: Add typed API contracts**

Define `SettlementInstructionDirection`, `SettlementInstructionStatus`, `SettlementInstructionActionType`, query/list/detail/action types, and generate/edit/command/outcome commands. Keep `id`, source IDs, actor IDs, signed net, and amount as strings; `expectedVersion/version` are numbers. Every mutation sets `headers: { exposeBusinessCode: 'true' }`.

- [ ] **Step 4: Build the unified workbench**

Implement the approved layout: filter form, unified dense table, direction/status text, permission-aware primary action, generate dialog with closed-batch lookup, full-width responsive detail drawer, immutable source snapshot, masked counterparty data, review checks, action timeline, and dedicated edit/review/reject/cancel/execute/collect/waive forms. Use existing Element Plus and app hooks; do not add dependencies or nested cards.

- [ ] **Step 5: Add complete states and source navigation**

Implement loading, initial empty, filtered empty, list/detail error, permission denial, command progress/success/failure, duplicate resolution, version-conflict refresh, and self-review disabled explanation. Add Phase 45/46 navigation using a string-safe `batchId` query on the instruction workbench; source pages remain usable when no instruction exists or the batch is rejected by the tenant-scoped lookup.

- [ ] **Step 6: Add bilingual copy and responsive rules**

At `390px`, stack filters, keep table overflow local, make drawer full-width, wrap long identifiers, and reserve stable action-button dimensions. Run:

```powershell
pnpm --dir admin-ui check:payment-settlement-instruction
pnpm --dir admin-ui check:payment-settlement
pnpm --dir admin-ui check:payment-settlement-report
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui exec eslint src/views/payment/payment-settlement-instruction/index.vue src/api/payment/paymentSettlementInstruction/index.ts src/api/payment/paymentSettlementInstruction/types.ts
```

Expected: all commands exit `0`.

- [ ] **Step 7: Commit**

```powershell
git add admin-ui/package.json admin-ui/scripts/check-payment-settlement-instruction-contract.mjs admin-ui/src/api/payment/paymentSettlementInstruction admin-ui/src/views/payment/payment-settlement-instruction admin-ui/src/views/payment/payment-settlement/index.vue admin-ui/src/views/payment/payment-settlement-report/index.vue admin-ui/src/lang/en_US.ts admin-ui/src/lang/zh_CN.ts
git commit -m "feat: add settlement instruction workbench"
```

### Task 7: Regression And Production-Equivalent Builds

**Files:**
- Modify only if an in-scope regression proves a defect.
- Modify: `progress.md`

- [ ] **Step 1: Run focused payment regression**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementInstruction*,PaymentSettlementReport*,PaymentSettlement*,PaymentReconciliation*,PurchaseReversalReview*' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: zero failures, errors, and skips; record the exact count.

- [ ] **Step 2: Run split wallet/member/payment recovery regression**

Reuse the Phase 46 fresh-JVM commands for wallet `19/19`, member `7/7`, and payment fulfillment/reversal `45/45`. Expected: all pass without shared-JVM native-memory exhaustion.

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

Scan Phase 47 production files for `IWallet`, wallet mappers, payment command services, raw bodies, signatures, private keys, full account fields, filesystem writes, and SQL mutation of non-Phase-47 tables. Run all frontend contracts, i18n, targeted ESLint, and `git diff --check`.

- [ ] **Step 5: Record and commit build evidence**

Append exact counts/modules/warnings to `progress.md`, then:

```powershell
git add progress.md
git commit -m "test: verify settlement instruction builds"
```

### Task 8: SQL, Runtime, Concurrency, And Responsive Acceptance

**Files:**
- Create: `admin-ui/scripts/phase47-payment-settlement-instruction-runtime.mjs`
- Create: `docs/implementation/phase47-payment-settlement-instruction-desktop.png`
- Create: `docs/implementation/phase47-payment-settlement-instruction-mobile.png`
- Modify: this plan
- Modify: `progress.md`

- [ ] **Step 1: Import SQL twice and verify metadata**

Run `backend/script/bin/import-sql-utf8.ps1` twice. Query MySQL and assert exactly two Phase 47 tables, one page `2035`, nine permissions `20351`-`20359`, three dictionary types, unique dictionary values, and unchanged Phase 45/46 menu rows.

- [ ] **Step 2: Restart only verified project services**

Repackage if needed, stop only listeners whose command lines point to this workspace on `8080/5173/5174`, start backend with `local` profile and constrained JVM settings, then Admin and H5. Expected: all three return HTTP `200`.

- [ ] **Step 3: Create deterministic source fixtures and snapshots**

Create or reuse three tenant `000000` closed batches with positive, negative, and zero net values. Snapshot settlement, report source, purchase/payment event, reconciliation, reversal, member-risk, turnover, wallet-account, and wallet-transaction tables before Phase 47 commands.

- [ ] **Step 4: Verify generation, tenant isolation, and idempotency**

Generate all three instructions through authenticated endpoints. Assert exact direction/amount/status, one instruction per batch, deterministic replay, changed-payload conflict, cross-tenant invisibility, and concurrent generation convergence to one row/action.

- [ ] **Step 5: Verify payable review and external execution**

Edit/submit the positive instruction, prove creator self-review denial, reject as another operator, revise/resubmit, approve, then execute with external reference/time. Replay every command and force one stale version; assert stable responses, exact version increments, one log per successful request, and terminal protection.

- [ ] **Step 6: Verify receivable and balanced outcomes**

Collect one receivable fixture and, using a second isolated receivable fixture, verify separately authorized waiver with mandatory reason. Assert balanced is `NO_ACTION` with no command available. Confirm unauthorised execute/collect/waive calls are denied.

- [ ] **Step 7: Verify UI and immutable source state**

At `1440x900` and `390x844`, verify filters, unified table, exact money, directions, permission gating, self-review explanation, forms, source links, timeline, local table scrolling, no console errors, no page-level overflow, and nonblank pixels. Compare all source checksums before/after; only Phase 47 tables and sanitized operation logs may change.

- [ ] **Step 8: Final verification and completion record**

Re-run all Phase 47 backend tests, three frontend settlement contracts, i18n, targeted ESLint, safety scans, runtime script, screenshot pixel checks, and `git diff --check`. Mark every checkbox in this plan and append exact runtime evidence plus `Phase 47 completed` to `progress.md`; leave `task_plan.md` untouched.

- [ ] **Step 9: Commit runtime evidence**

```powershell
git add admin-ui/scripts/phase47-payment-settlement-instruction-runtime.mjs docs/implementation/phase47-payment-settlement-instruction-desktop.png docs/implementation/phase47-payment-settlement-instruction-mobile.png docs/superpowers/plans/2026-07-31-payment-settlement-instruction-review.md progress.md
git commit -m "test: verify settlement instruction runtime"
```
